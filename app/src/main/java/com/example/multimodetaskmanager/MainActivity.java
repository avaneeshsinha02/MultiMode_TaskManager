package com.example.multimodetaskmanager;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> taskList = new ArrayList<>();
    private TextView tvEmptyState;
    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();

    private int taskPositionToEdit = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("TaskPrefs", MODE_PRIVATE);

        setupCustomAppBar();
        setupRecyclerView();
        loadTasks();

        FloatingActionButton fab = findViewById(R.id.fabAddTask);
        fab.setOnClickListener(v -> showAddTaskDialog(null));

        setupItemTouchHelper();
    }

    private void setupCustomAppBar() {
        ImageButton btnSort = findViewById(R.id.btnSort);
        ImageButton btnTheme = findViewById(R.id.btnTheme);
        ImageButton btnLogout = findViewById(R.id.btnLogout);

        btnSort.setOnClickListener(v -> showSortMenu(v));
        btnTheme.setOnClickListener(v -> toggleTheme());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewTasks);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        if (isTablet(this)) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        adapter = new TaskAdapter(new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskChecked(Task task) {
                removeTask(task);
                Toast.makeText(MainActivity.this, R.string.task_marked_done, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onTaskLongClick(View v, int position) {
                taskPositionToEdit = position;
                v.showContextMenu();
            }
        });
        recyclerView.setAdapter(adapter);
        registerForContextMenu(recyclerView);
    }

    private void setupItemTouchHelper() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                removeTask(adapter.getTaskAt(position));
                Toast.makeText(MainActivity.this, R.string.task_deleted, Toast.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void showAddTaskDialog(Task taskToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        final TextInputEditText etTaskTitle = dialogView.findViewById(R.id.etTaskTitle);
        final Spinner spinnerPriority = dialogView.findViewById(R.id.spinnerPriority);
        final Button btnSelectDate = dialogView.findViewById(R.id.btnSelectDate);
        final Button btnSelectTime = dialogView.findViewById(R.id.btnSelectTime);

        final Calendar calendar = Calendar.getInstance();
        if (taskToEdit != null) {
            calendar.setTimeInMillis(taskToEdit.getDueDate());
            etTaskTitle.setText(taskToEdit.getTitle());
            spinnerPriority.setSelection(taskToEdit.getPriority());
        } else {
            calendar.add(Calendar.HOUR, 1);
        }

        btnSelectDate.setOnClickListener(v -> showDatePicker(calendar, btnSelectDate));
        btnSelectTime.setOnClickListener(v -> showTimePicker(calendar, btnSelectTime));

        builder.setTitle(taskToEdit == null ? R.string.add_task : R.string.edit_task);
        builder.setPositiveButton(R.string.save, null);
        builder.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String title = etTaskTitle.getText().toString().trim();
                if (TextUtils.isEmpty(title)) {
                    Toast.makeText(this, R.string.title_cannot_be_empty, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                    Toast.makeText(this, R.string.invalid_datetime, Toast.LENGTH_SHORT).show();
                    return;
                }

                int priority = spinnerPriority.getSelectedItemPosition();

                if (taskToEdit != null) {
                    taskToEdit.setTitle(title);
                    taskToEdit.setPriority(priority);
                    taskToEdit.setDueDate(calendar.getTimeInMillis());
                    updateTask(taskToEdit);
                } else {
                    Task newTask = new Task(title, calendar.getTimeInMillis(), priority);
                    addTask(newTask);
                }
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showDatePicker(Calendar calendar, Button btn) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    btn.setText(String.format("%d/%d/%d", dayOfMonth, month + 1, year));
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker(Calendar calendar, Button btn) {
        new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    btn.setText(String.format("%02d:%02d", hourOfDay, minute));
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void addTask(Task task) {
        taskList.add(task);
        updateAndSaveTasks();
        Toast.makeText(this, R.string.task_added_successfully, Toast.LENGTH_SHORT).show();
    }

    private void updateTask(Task task) {
        updateAndSaveTasks();
        Toast.makeText(this, R.string.task_updated_successfully, Toast.LENGTH_SHORT).show();
    }

    private void removeTask(Task task) {
        taskList.remove(task);
        updateAndSaveTasks();
    }

    private void saveTasks() {
        String json = gson.toJson(taskList);
        sharedPreferences.edit().putString("tasks", json).apply();
    }

    private void loadTasks() {
        String json = sharedPreferences.getString("tasks", null);
        Type type = new TypeToken<ArrayList<Task>>() {}.getType();
        taskList = gson.fromJson(json, type);
        if (taskList == null) {
            taskList = new ArrayList<>();
        }
        updateAndSaveTasks();
    }

    private void updateAndSaveTasks() {
        adapter.submitList(new ArrayList<>(taskList));
        tvEmptyState.setVisibility(taskList.isEmpty() ? View.VISIBLE : View.GONE);
        saveTasks();
    }

    private void showSortMenu(View anchor) {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(this, getCurrentThemeResId());
        PopupMenu popup = new PopupMenu(wrapper, anchor);
        popup.getMenuInflater().inflate(R.menu.sort_popup_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.sort_priority) {
                Collections.sort(taskList, (t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()));
            } else if (itemId == R.id.sort_due_date) {
                Collections.sort(taskList, (t1, t2) -> Long.compare(t1.getDueDate(), t2.getDueDate()));
            } else if (itemId == R.id.sort_name) {
                Collections.sort(taskList, (t1, t2) -> t1.getTitle().compareToIgnoreCase(t2.getTitle()));
            }
            adapter.submitList(new ArrayList<>(taskList));
            return true;
        });
        popup.show();
    }

    private int getCurrentThemeResId() {

        return R.style.AppTheme_Light;
    }

    private void toggleTheme() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String currentTheme = pref.getString("theme", "light");
        SharedPreferences.Editor editor = pref.edit();
        if (currentTheme.equals("light")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            editor.putString("theme", "dark");
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            editor.putString("theme", "light");
        }
        editor.apply();
    }

    private void logout() {
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.apply();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onCreateContextMenu(@NonNull android.view.ContextMenu menu, @NonNull View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.task_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if(taskPositionToEdit < 0 || taskPositionToEdit >= adapter.getCurrentList().size()){
            return super.onContextItemSelected(item);
        }
        Task task = adapter.getTaskAt(taskPositionToEdit);
        int itemId = item.getItemId();
        if (itemId == R.id.context_edit) {
            showAddTaskDialog(task);
            return true;
        } else if (itemId == R.id.context_delete) {
            removeTask(task);
            Toast.makeText(this, R.string.task_deleted, Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.context_mark_done) {
            removeTask(task);
            Toast.makeText(this, R.string.task_marked_done, Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}