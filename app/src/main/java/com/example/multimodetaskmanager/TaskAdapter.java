package com.example.multimodetaskmanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    private final OnTaskClickListener listener;

    protected TaskAdapter(OnTaskClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK = new DiffUtil.ItemCallback<Task>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getDueDate() == newItem.getDueDate() &&
                    oldItem.getPriority() == newItem.getPriority();
        }
    };

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task currentTask = getItem(position);
        holder.bind(currentTask, listener);
    }

    public Task getTaskAt(int position) {
        return getItem(position);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTaskTitle, tvTaskDueDate;
        private final View viewPriority;
        private final CheckBox checkBoxDone;
        private final Context context;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskDueDate = itemView.findViewById(R.id.tvTaskDueDate);
            viewPriority = itemView.findViewById(R.id.viewPriority);
            checkBoxDone = itemView.findViewById(R.id.checkbox_done);
        }

        public void bind(final Task task, final OnTaskClickListener listener) {
            tvTaskTitle.setText(task.getTitle());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault());
            tvTaskDueDate.setText(sdf.format(new Date(task.getDueDate())));

            int priorityColor;
            switch (task.getPriority()) {
                case 2:
                    priorityColor = ContextCompat.getColor(context, R.color.priority_high);
                    break;
                case 1:
                    priorityColor = ContextCompat.getColor(context, R.color.priority_medium);
                    break;
                default:
                    priorityColor = ContextCompat.getColor(context, R.color.priority_low);
                    break;
            }
            viewPriority.setBackgroundColor(priorityColor);

            checkBoxDone.setChecked(false);

            checkBoxDone.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onTaskChecked(task);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onTaskLongClick(v, getAdapterPosition());
                    return true;
                }
                return false;
            });
        }
    }

    public interface OnTaskClickListener {
        void onTaskChecked(Task task);
        void onTaskLongClick(View v, int position);
    }
}