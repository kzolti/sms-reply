package com.example.smsreply

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TemplatesActivity : AppCompatActivity() {

    private lateinit var adapter: TemplatesAdapter
    private lateinit var recyclerView: RecyclerView
    private val TAG = "TemplatesActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_templates)

        recyclerView = findViewById(R.id.recyclerViewTemplates)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = TemplatesAdapter(
            templates = emptyList(),
            onSelect = { id ->
                Log.d(TAG, "Selecting template: $id")
                MessageRepository.selectTemplate(this, id)
                loadTemplates()
            },
            onEdit = { id ->
                Log.d(TAG, "Editing template: $id")
                val template = MessageRepository.getTemplates(this).find { it.id == id }
                if (template != null) {
                    showEditDialog(template)
                } else {
                    Log.e(TAG, "Template not found for editing: $id")
                }
            },
            onDelete = { id ->
                Log.d(TAG, "Deleting template: $id")
                val template = MessageRepository.getTemplates(this).find { it.id == id }
                if (template != null) {
                    showDeleteConfirmDialog(template)
                } else {
                    Log.e(TAG, "Template not found for deletion: $id")
                }
            }
        )
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showEditDialog(null)
        }

        loadTemplates()
    }

    private fun loadTemplates() {
        try {
            val templates = MessageRepository.getTemplates(this)
            adapter.updateData(templates)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading templates", e)
        }
    }

    private fun showEditDialog(template: MessageTemplate?) {
        try {
            val input = EditText(this)
            input.hint = getString(R.string.enter_sms_hint)
            if (template != null) {
                input.setText(template.text)
            }

            val container = FrameLayout(this)
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            params.leftMargin = 50
            params.rightMargin = 50
            input.layoutParams = params
            container.addView(input)

            AlertDialog.Builder(this)
                .setTitle(if (template == null) getString(R.string.new_message_title) else getString(R.string.edit_message_title))
                .setView(container)
                .setPositiveButton(getString(R.string.save_button)) { _, _ ->
                    val text = input.text.toString().trim()
                    if (text.isNotBlank()) {
                        if (template == null) {
                            MessageRepository.addTemplate(this, text)
                            Log.d(TAG, "Added new template: $text")
                        } else {
                            MessageRepository.updateTemplate(this, template.id, text)
                            Log.d(TAG, "Updated template ${template.id}: $text")
                        }
                        loadTemplates()
                    } else {
                        Log.w(TAG, "Empty message text, not saving")
                    }
                }
                .setNegativeButton(getString(R.string.cancel_button), null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing edit dialog", e)
        }
    }

    private fun showDeleteConfirmDialog(template: MessageTemplate) {
        try {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_message_title))
                .setMessage("${getString(R.string.delete_message_confirm)}\n\n\"${template.text.take(50)}${if (template.text.length > 50) "..." else ""}\"")
                .setPositiveButton(getString(R.string.delete_button)) { _, _ ->
                    MessageRepository.deleteTemplate(this, template.id)
                    Log.d(TAG, "Deleted template: ${template.id}")
                    loadTemplates()
                }
                .setNegativeButton(getString(R.string.cancel_button), null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing delete dialog", e)
        }
    }
}
