package com.appvin.ai

import android.graphics.Bitmap
import com.appvin.data.ChatMessage
import com.appvin.data.Wine
import com.appvin.data.formatSources
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import org.json.JSONObject
import java.time.LocalDate

/** All Gemini calls. They run on Vertex AI / Agent Platform (location "global", billed to the linked Google Cloud project) via Firebase AI Logic. */
object Sommelier {
    // Stable model; retirement no earlier than 2027-05-19.
    private const val MODEL = "gemini-3.5-flash"
    const val CURRENCY = "EUR"

    private val ai by lazy { Firebase.ai(backend = GenerativeBackend.agentPlatform()) }

    data class LabelInfo(
        val name: String,
        val producer: String,
        val vintage: Int?,
        val color: String,
        val country: String,
        val region: String,
        val appellation: String,
        val grapes: String,
        val bottleSize: String,
        val alcohol: String,
    )

    data class Research(
        val drinkFrom: Int?,
        val drinkUntil: Int?,
        val peakYear: Int?,
        val valuePerBottle: Double?,
        val description: String,
        val foodPairings: String,
        val servingTips: String,
        val grapes: String,
        val sources: String,
    )

    data class Reply(val text: String, val sources: String, val searchSuggestionsHtml: String?)

    private val labelSchema = Schema.obj(
        mapOf(
            "name" to Schema.string("Wine name or cuvée, without the producer"),
            "producer" to Schema.string("Winery, château or domaine"),
            "vintage" to Schema.integer("Harvest year, or 0 if non-vintage or not visible"),
            "color" to Schema.enumeration(listOf("Red", "White", "Rosé", "Sparkling", "Dessert", "Fortified", "Orange")),
            "country" to Schema.string(),
            "region" to Schema.string(),
            "appellation" to Schema.string(),
            "grapes" to Schema.string("Comma-separated grape varieties, from the label or common knowledge of this wine"),
            "bottleSize" to Schema.string("For example 750 ml"),
            "alcohol" to Schema.string("For example 13.5%"),
        )
    )

    /** Step 1: read the label photo. */
    suspend fun readLabel(photo: Bitmap): LabelInfo {
        val model = ai.generativeModel(
            modelName = MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = labelSchema
            },
        )
        val response = model.generateContent(
            content {
                image(photo)
                text(
                    "This is a photo of a wine bottle label. Extract the wine's details. " +
                        "Use an empty string for anything you cannot determine."
                )
            }
        )
        val json = JSONObject(response.text ?: error("Gemini returned no answer"))
        return LabelInfo(
            name = json.optString("name"),
            producer = json.optString("producer"),
            vintage = json.optInt("vintage").takeIf { it > 0 },
            color = json.optString("color"),
            country = json.optString("country"),
            region = json.optString("region"),
            appellation = json.optString("appellation"),
            grapes = json.optString("grapes"),
            bottleSize = json.optString("bottleSize").ifBlank { "750 ml" },
            alcohol = json.optString("alcohol"),
        )
    }

    /** Step 2: look the wine up on Google Search for drinking window, value and pairings. */
    suspend fun research(wine: Wine): Research {
        val model = ai.generativeModel(modelName = MODEL, tools = listOf(Tool.googleSearch()))
        val vintage = wine.vintage?.toString() ?: "non-vintage"
        val response = model.generateContent(
            """
            Search the web for this wine and report current information about it.
            Wine: ${wine.producer} ${wine.name}, vintage $vintage
            Region: ${listOf(wine.appellation, wine.region, wine.country).filter { it.isNotBlank() }.joinToString(", ")}
            Colour: ${wine.color}. Grapes: ${wine.grapes}. Bottle: ${wine.bottleSize}.

            Today is ${LocalDate.now()}. Use critic and merchant drinking-window recommendations for this exact
            vintage where available, and current retail or auction prices in $CURRENCY for one bottle of this size.

            Reply with ONLY a JSON object, no other text, with these keys:
            "drinkFrom" (year, integer), "drinkUntil" (year, integer), "peakYear" (year, integer),
            "valuePerBottle" (number in $CURRENCY, typical current price), "description" (2-3 sentences on style and taste),
            "foodPairings" (comma-separated dishes), "servingTips" (temperature, decanting, glass),
            "grapes" (comma-separated). Use null for anything you cannot find.
            """.trimIndent()
        )
        val json = JSONObject(extractJson(response.text ?: error("Gemini returned no answer")))
        return Research(
            drinkFrom = json.optYear("drinkFrom"),
            drinkUntil = json.optYear("drinkUntil"),
            peakYear = json.optYear("peakYear"),
            valuePerBottle = json.optDouble("valuePerBottle").takeIf { !it.isNaN() && it > 0 },
            description = json.optText("description"),
            foodPairings = json.optText("foodPairings"),
            servingTips = json.optText("servingTips"),
            grapes = json.optText("grapes"),
            sources = sourcesOf(response),
        )
    }

    /** The sommelier chat. [cellar] describes the bottles the owner has; history ends with the new user message. */
    suspend fun chat(history: List<ChatMessage>, cellar: String): Reply {
        val model = ai.generativeModel(
            modelName = MODEL,
            tools = listOf(Tool.googleSearch()),
            systemInstruction = content {
                text(
                    """
                    You are a warm, knowledgeable personal sommelier for the owner of a private wine cellar.
                    Today is ${LocalDate.now()}. Help them choose what to drink, pair food, and understand their wines.
                    Always recommend bottles from their own cellar first, naming the exact wine and where it is stored,
                    and prefer wines that are in or near their drinking window. Only suggest buying something if nothing fits.
                    Use Google Search when you need current facts such as vintage reports, prices or reviews.
                    Answer in the language the user writes in. Keep answers short and easy to read; use simple lists.

                    The owner's cellar right now:
                    $cellar
                    """.trimIndent()
                )
            },
        )
        val contents = history.takeLast(30).map { message ->
            content(role = if (message.fromUser) "user" else "model") { text(message.text) }
        }
        val response = model.generateContent(contents)
        val metadata = response.candidates.firstOrNull()?.groundingMetadata
        return Reply(
            text = response.text ?: "Sorry, I could not come up with an answer. Please try again.",
            sources = sourcesOf(response),
            searchSuggestionsHtml = metadata?.searchEntryPoint?.renderedContent,
        )
    }

    private fun sourcesOf(response: GenerateContentResponse): String =
        formatSources(
            response.candidates.firstOrNull()?.groundingMetadata?.groundingChunks.orEmpty()
                .mapNotNull { chunk -> chunk.web?.let { (it.title ?: it.uri ?: "") to (it.uri ?: "") } }
        )

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        require(start >= 0 && end > start) { "Gemini did not return the wine details" }
        return text.substring(start, end + 1)
    }

    private fun JSONObject.optYear(key: String): Int? = optInt(key).takeIf { it in 1900..2200 }

    private fun JSONObject.optText(key: String): String = if (isNull(key)) "" else optString(key)
}
