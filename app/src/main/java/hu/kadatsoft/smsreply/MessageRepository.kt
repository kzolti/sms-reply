package hu.kadatsoft.smsreply

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class MessageTemplate(
    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var isSelected: Boolean = false
)

object MessageRepository {
    private const val PREFS_NAME = "SmsTemplates"
    private const val KEY_TEMPLATES = "templates"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getTemplates(context: Context): List<MessageTemplate> {
        val jsonString = getPrefs(context).getString(KEY_TEMPLATES, null) ?: return getDefaultTemplates(context)
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<MessageTemplate>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(MessageTemplate(
                    id = obj.getString("id"),
                    text = obj.getString("text"),
                    isSelected = obj.getBoolean("isSelected")
                ))
            }
            
            // Ensure at least one template is selected
            if (list.isNotEmpty() && list.none { it.isSelected }) {
                list[0].isSelected = true
                saveTemplates(context, list)
            }
            
            if (list.isEmpty()) getDefaultTemplates(context) else list
        } catch (e: Exception) {
            getDefaultTemplates(context)
        }
    }

    private fun getDefaultTemplates(context: Context): List<MessageTemplate> {
        return listOf(
            MessageTemplate(
                text = context.getString(R.string.default_template_text),
                isSelected = true
            )
        )
    }

    private fun saveTemplates(context: Context, templates: List<MessageTemplate>) {
        // Ensure at least one template is selected before saving
        val templatesWithSelection = templates.toMutableList()
        if (templatesWithSelection.isNotEmpty() && templatesWithSelection.none { it.isSelected }) {
            templatesWithSelection[0].isSelected = true
        }
        
        val jsonArray = JSONArray()
        templatesWithSelection.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("text", it.text)
            obj.put("isSelected", it.isSelected)
            jsonArray.put(obj)
        }
        getPrefs(context).edit().putString(KEY_TEMPLATES, jsonArray.toString()).apply()
    }

    fun addTemplate(context: Context, text: String) {
        val templates = getTemplates(context).toMutableList()
        val newTemplate = MessageTemplate(text = text, isSelected = templates.isEmpty())
        templates.add(newTemplate)
        
        // Ensure at least one template is always selected
        if (templates.none { it.isSelected }) {
            newTemplate.isSelected = true
        }
        
        saveTemplates(context, templates)
    }

    fun deleteTemplate(context: Context, id: String) {
        val templates = getTemplates(context).toMutableList()
        val wasSelected = templates.find { it.id == id }?.isSelected == true
        templates.removeAll { it.id == id }
        
        // If we deleted the selected one, select the first one if available
        if (wasSelected && templates.isNotEmpty()) {
            templates[0].isSelected = true
        }
        
        // Ensure at least one template is always selected if any exist
        if (templates.isNotEmpty() && templates.none { it.isSelected }) {
            templates[0].isSelected = true
        }
        
        saveTemplates(context, templates)
    }

    fun updateTemplate(context: Context, id: String, newText: String) {
        val templates = getTemplates(context).toMutableList()
        templates.find { it.id == id }?.let { template ->
            val wasSelected = template.isSelected
            template.text = newText
            // Keep the selection state when updating
            template.isSelected = wasSelected
            saveTemplates(context, templates)
        }
    }

    fun selectTemplate(context: Context, id: String) {
        val templates = getTemplates(context).toMutableList()
        templates.forEach { it.isSelected = (it.id == id) }
        saveTemplates(context, templates)
    }

    fun getSelectedMessage(context: Context): String {
        return getTemplates(context).find { it.isSelected }?.text 
            ?: getDefaultTemplates(context).first().text
    }
}
