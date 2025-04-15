import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class HabitAdapter(
    private val habits: List<Habit>,
    private val onItemClick: (Habit) -> Unit,
    private val onItemLongClick: (Habit) -> Unit,
    private val saveHabitsToPrefs: () -> Unit
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val habitCheck: CheckBox = itemView.findViewById(R.id.habitCheck)
        val habitTitle: TextView = itemView.findViewById(R.id.habitTitle)
        val habitTime: TextView = itemView.findViewById(R.id.habitTime)

        init {
            itemView.setOnClickListener {
                onItemClick(habits[adapterPosition])
            }
            itemView.setOnLongClickListener {
                onItemLongClick(habits[adapterPosition])
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        holder.habitTitle.text = habit.name
        holder.habitTime.text = habit.time
        holder.habitCheck.isChecked = habit.isChecked

        if (habit.isChecked) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.checked_habit_color))
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.unchecked_habit_color))
        }

        // Update the habit's isChecked state when the checkbox is clicked
        holder.habitCheck.setOnCheckedChangeListener { _, isChecked ->
            habit.isChecked = isChecked

            if (isChecked) {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.checked_habit_color))
            } else {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.unchecked_habit_color))
            }

            saveHabitsToPrefs() // Call the save method passed from MainActivity
        }
    }

    // Return the total number of items
    override fun getItemCount(): Int = habits.size
}
