package hu.kadatsoft.smsreply

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TemplatesAdapter(
    private var templates: List<MessageTemplate>,
    private val onSelect: (String) -> Unit,
    private val onEdit: (String) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<TemplatesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val radioButtonSelect: RadioButton = view.findViewById(R.id.radioButtonSelect)
        val textViewMessage: TextView = view.findViewById(R.id.textViewMessage)
        val buttonEdit: ImageButton = view.findViewById(R.id.buttonEdit)
        val buttonDelete: ImageButton = view.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_template, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val template = templates[position]
        holder.textViewMessage.text = template.text
        holder.radioButtonSelect.isChecked = template.isSelected

        // Clear previous listeners to avoid issues
        holder.radioButtonSelect.setOnClickListener(null)
        holder.textViewMessage.setOnClickListener(null)
        holder.buttonEdit.setOnClickListener(null)
        holder.buttonDelete.setOnClickListener(null)

        // Set new listeners
        holder.radioButtonSelect.setOnClickListener {
            android.util.Log.d("TemplatesAdapter", "Radio button clicked for template: ${template.id}")
            onSelect(template.id)
        }
        
        // Also allow clicking the text to select
        holder.textViewMessage.setOnClickListener {
            android.util.Log.d("TemplatesAdapter", "Text clicked for template: ${template.id}")
            onSelect(template.id)
        }

        holder.buttonEdit.setOnClickListener {
            android.util.Log.d("TemplatesAdapter", "Edit button clicked for template: ${template.id}")
            onEdit(template.id)
        }

        holder.buttonDelete.setOnClickListener {
            android.util.Log.d("TemplatesAdapter", "Delete button clicked for template: ${template.id}")
            onDelete(template.id)
        }
    }

    override fun getItemCount() = templates.size

    fun updateData(newTemplates: List<MessageTemplate>) {
        templates = newTemplates
        notifyDataSetChanged()
    }
}
