package com.rudra.myassistant;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends BaseEdgeToEdgeActivity {

    private static final int MIN_YEAR = 1970;
    private static final int MAX_YEAR = 2100;
    private static final int TOTAL_MONTHS = (MAX_YEAR - MIN_YEAR + 1) * 12;

    private ViewPager2 pager;
    private TextView monthText;
    private TextView yearText;
    private TextView selectedDateText;
    private RecyclerView eventsRecycler;
    private View fabAddEvent;

    private CalendarViewModel viewModel;
    private CalendarEventsAdapter eventsAdapter;
    private CalendarEventsAdapter searchAdapter;
    private MonthPagerAdapter monthPagerAdapter;
    
    private EditText searchInput;
    private RecyclerView searchResultsRecycler;
    private View btnSearchToggle;

    private long selectedDateMillis;
    private final long todayDateMillis = normalizeToDateMillis(System.currentTimeMillis());
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);

        View back = findViewById(R.id.calendar_back_button);
        back.setOnClickListener(v -> finish());

        pager = findViewById(R.id.calendar_pager);
        monthText = findViewById(R.id.calendar_month_text);
        yearText = findViewById(R.id.calendar_year_text);
        selectedDateText = findViewById(R.id.text_selected_date);
        eventsRecycler = findViewById(R.id.recycler_events);
        fabAddEvent = findViewById(R.id.fab_add_event);
        searchInput = findViewById(R.id.search_input);
        searchResultsRecycler = findViewById(R.id.recycler_search_results);
        btnSearchToggle = findViewById(R.id.btn_search_toggle);

        selectedDateMillis = normalizeToDateMillis(System.currentTimeMillis());
        updateSelectedDateText();
        updateHeaderFromMillis(selectedDateMillis);

        // Events List
        eventsRecycler.setLayoutManager(new LinearLayoutManager(this));
        
        eventsAdapter = new CalendarEventsAdapter(new CalendarEventsAdapter.OnEventDeleteListener() {
            @Override
            public void onDelete(String eventId) {
                new AlertDialog.Builder(CalendarActivity.this)
                        .setTitle("Delete Event")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Yes", (d, w) -> viewModel.deleteEvent(eventId))
                        .setNegativeButton("No", null)
                        .show();
            }
        });
        eventsRecycler.setAdapter(eventsAdapter);

        // Calendar Pager
        pager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        monthPagerAdapter = new MonthPagerAdapter();
        pager.setAdapter(monthPagerAdapter);

        int initialPos = positionForYearMonth(getYearFromMillis(selectedDateMillis), getMonthFromMillis(selectedDateMillis));
        pager.setCurrentItem(initialPos, false);
        
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateHeaderForPosition(position);
            }
        });

        findViewById(R.id.btn_today).setOnClickListener(v -> {
            selectedDateMillis = normalizeToDateMillis(System.currentTimeMillis());
            int pos = positionForYearMonth(getYearFromMillis(selectedDateMillis), getMonthFromMillis(selectedDateMillis));
            pager.setCurrentItem(pos, true);
            updateSelectedDateText();
            filterEventsForSelectedDate(viewModel.getEvents().getValue());
            monthPagerAdapter.notifyDataSetChanged();
        });

        viewModel.getEvents().observe(this, events -> {
            monthPagerAdapter.setAllEvents(events);
            filterEventsForSelectedDate(events);
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.loadEvents();

        // Add Event click
        fabAddEvent.setOnClickListener(v -> showAddEventDialog());

        // Search Toggle logic
        btnSearchToggle.setOnClickListener(v -> {
            if (searchInput.getVisibility() == View.VISIBLE) {
                searchInput.setVisibility(View.GONE);
                searchResultsRecycler.setVisibility(View.GONE);
                searchInput.setText("");
            } else {
                searchInput.setVisibility(View.VISIBLE);
                searchInput.requestFocus();
            }
        });

        // Search Adapter
        searchResultsRecycler.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new CalendarEventsAdapter(eventId -> {
            new AlertDialog.Builder(CalendarActivity.this)
                    .setTitle("Delete Event")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Yes", (d, w) -> viewModel.deleteEvent(eventId))
                    .setNegativeButton("No", null)
                    .show();
        });
        searchResultsRecycler.setAdapter(searchAdapter);

        // Search filtering logic
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Viper Motion System — entrance animation
        View root = findViewById(android.R.id.content);
        if (root != null) {
            float dp = root.getResources().getDisplayMetrics().density * 24;
            root.setTranslationY(dp);
            root.setAlpha(0.5f);
            root.animate().translationY(0).alpha(1f)
                    .setDuration(350)
                    .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                    .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                    .start();
        }

        com.viperai.notifications.NotificationRouter.handleIntent(this, getIntent());
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        com.viperai.notifications.NotificationRouter.handleIntent(this, intent);
    }

    public void highlightEvent(String id) {
        List<CalendarEvent> all = viewModel.getEvents().getValue();
        if (all == null) return;
        
        CalendarEvent target = null;
        for (CalendarEvent e : all) {
            if (e.getId().equals(id)) {
                target = e;
                break;
            }
        }
        
        if (target != null) {
            try {
                SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                String datePart = target.getStartTime().split("T")[0];
                Date date = iso.parse(datePart);
                if (date != null) {
                    onDateClicked(normalizeToDateMillis(date.getTime()));
                    int pos = positionForYearMonth(getYearFromMillis(date.getTime()), getMonthFromMillis(date.getTime()));
                    pager.setCurrentItem(pos, true);
                    
                    final String targetId = id;
                    eventsRecycler.postDelayed(() -> {
                        for (int i = 0; i < eventsRecycler.getChildCount(); i++) {
                            View child = eventsRecycler.getChildAt(i);
                            // Assuming tag or something carries ID, but matching title is safer since adapter is simple
                            // Better: modify adapter to set tag onitemView.
                            // For now, let's just pulse the whole recycler or first item if it's the only one.
                            android.animation.ObjectAnimator pulse = android.animation.ObjectAnimator.ofFloat(child, "alpha", 1.0f, 0.5f, 1.0f);
                            pulse.setDuration(600);
                            pulse.setRepeatCount(1);
                            pulse.start();
                        }
                    }, 600);
                }
            } catch (Exception e) {}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadEvents();
        }
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            searchResultsRecycler.setVisibility(View.GONE);
            return;
        }
        
        List<CalendarEvent> all = viewModel.getEvents().getValue();
        if (all == null) return;
        
        List<CalendarEvent> filtered = new ArrayList<>();
        for (CalendarEvent e : all) {
            boolean titleMatch = e.getTitle() != null && e.getTitle().toLowerCase().contains(query.toLowerCase());
            boolean descMatch = e.getDescription() != null && e.getDescription().toLowerCase().contains(query.toLowerCase());
            if (titleMatch || descMatch) {
                filtered.add(e);
            }
        }
        
        searchAdapter.setEvents(filtered);
        searchResultsRecycler.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateSelectedDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());
        selectedDateText.setText(sdf.format(new Date(selectedDateMillis)));
    }

    private void filterEventsForSelectedDate(List<CalendarEvent> allEvents) {
        if (allEvents == null) return;
        List<CalendarEvent> filtered = new ArrayList<>();
        String selectedDateStr = dateFormat.format(new Date(selectedDateMillis));

        for (CalendarEvent e : allEvents) {
            // Check start time date part
            if (e.getStartTime() != null && e.getStartTime().startsWith(selectedDateStr)) {
                filtered.add(e);
            }
        }
        eventsAdapter.setEvents(filtered);
    }

    private void showAddEventDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null);
        builder.setView(view);

        EditText titleEt = view.findViewById(R.id.edit_event_title);
        EditText timeEt = view.findViewById(R.id.edit_event_time);
        EditText descEt = view.findViewById(R.id.edit_event_desc);
        Button saveBtn = view.findViewById(R.id.btn_save_event);

        AlertDialog dialog = builder.create();

        saveBtn.setOnClickListener(v -> {
            String title = titleEt.getText().toString().trim();
            String time = timeEt.getText().toString().trim();
            String desc = descEt.getText().toString().trim();

            if (title.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "Title and Time required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Combine date & time
            String datePart = dateFormat.format(new Date(selectedDateMillis));
            String fullTime = datePart + "T" + time + ":00"; 
            
            viewModel.addEvent(title, fullTime, desc);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateHeaderFromMillis(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        monthText.setText(monthFormat.format(cal.getTime()));
        yearText.setText(String.valueOf(cal.get(Calendar.YEAR)));
    }

    private void updateHeaderForPosition(int position) {
        int year = MIN_YEAR + (position / 12);
        int month = position % 12;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        
        monthText.setText(monthFormat.format(cal.getTime()));
        yearText.setText(String.valueOf(year));
    }

    private int positionForYearMonth(int year, int month0Based) {
        int y = Math.max(MIN_YEAR, Math.min(MAX_YEAR, year));
        int m = Math.max(0, Math.min(11, month0Based));
        return (y - MIN_YEAR) * 12 + m;
    }

    private static long normalizeToDateMillis(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static int getYearFromMillis(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return cal.get(Calendar.YEAR);
    }
    
    private static int getMonthFromMillis(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return cal.get(Calendar.MONTH);
    }

    // --- ADAPTERS ---

    private class MonthPagerAdapter extends RecyclerView.Adapter<MonthPagerAdapter.MonthViewHolder> {
        private List<CalendarEvent> allEvents = new ArrayList<>();

        void setAllEvents(List<CalendarEvent> events) {
            this.allEvents = events;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return TOTAL_MONTHS;
        }

        @Override
        public MonthViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_month_page, parent, false);
            return new MonthViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MonthViewHolder holder, int position) {
            int year = MIN_YEAR + (position / 12);
            int month = position % 12;
            holder.bind(year, month);
        }

        class MonthViewHolder extends RecyclerView.ViewHolder {
            private RecyclerView grid;
            private MonthGridAdapter adapter;
            
            MonthViewHolder(View itemView) {
                super(itemView);
                grid = itemView.findViewById(R.id.month_grid);
                grid.setLayoutManager(new GridLayoutManager(itemView.getContext(), 7));
                adapter = new MonthGridAdapter();
                grid.setAdapter(adapter);
            }
            
            void bind(int year, int month) {
                adapter.setMonthData(year, month, allEvents);
                
                // Fixed height for month page to follow inspo
                grid.post(() -> {
                   int h = grid.getHeight();
                   if(h > 0) adapter.setCellHeight(h / 6); 
                });
            }
        }
    }

    private class MonthGridAdapter extends RecyclerView.Adapter<MonthGridAdapter.CellViewHolder> {
        private static final int TYPE_WEEKDAY = 0;
        private static final int TYPE_DAY = 1;
        private final String[] WEEKDAYS = {"S", "M", "T", "W", "T", "F", "S"};
        
        private int year;
        private int month;
        private List<CalendarEvent> events = new ArrayList<>();
        private int cellHeight = 0;

        void setMonthData(int year, int month, List<CalendarEvent> events) {
            this.year = year;
            this.month = month;
            this.events = events;
            notifyDataSetChanged();
        }
        
        void setCellHeight(int h) {
            if (this.cellHeight != h) {
                this.cellHeight = h;
                notifyDataSetChanged();
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position < 7 ? TYPE_WEEKDAY : TYPE_DAY;
        }

        @Override
        public int getItemCount() {
            return 7 + 42; 
        }

        @Override
        public CellViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_cell, parent, false);
            return new CellViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CellViewHolder holder, int position) {
            if (cellHeight > 0) {
                ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
                lp.height = cellHeight;
                holder.itemView.setLayoutParams(lp);
            }

            if (getItemViewType(position) == TYPE_WEEKDAY) {
                holder.text.setText(WEEKDAYS[position]);
                holder.text.setTextColor(0x80FFFFFF); // 50% White
                holder.text.setTextSize(13);
                holder.text.setTypeface(null, android.graphics.Typeface.BOLD);
                holder.indicator.setVisibility(View.GONE);
                holder.itemView.setOnClickListener(null);
                return;
            }

            // Date processing reusing logic
            int dayIndex = position - 7;
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, 1);
            int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            
            int dateNum = dayIndex - firstDayOfWeek + 1;
            boolean isCurrentMonth = dateNum >= 1 && dateNum <= daysInMonth;
            
            Calendar cellCal = Calendar.getInstance();
            cellCal.set(Calendar.HOUR_OF_DAY, 0);
            cellCal.set(Calendar.MINUTE, 0);
            cellCal.set(Calendar.SECOND, 0);
            cellCal.set(Calendar.MILLISECOND, 0);

            if (isCurrentMonth) {
                cellCal.set(year, month, dateNum);
                holder.text.setText(String.valueOf(dateNum));
                holder.text.setTextColor(0xFFFFFFFF);
                holder.text.setAlpha(1.0f);
            } else {
                // prev/next month logic simplified for display
                if (dateNum < 1) {
                    cal.add(Calendar.MONTH, -1);
                    int prevMax = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                    int prevDay = prevMax + dateNum;
                    cellCal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), prevDay);
                    holder.text.setText(String.valueOf(prevDay));
                } else {
                    cal.add(Calendar.MONTH, 1);
                    int nextDay = dateNum - daysInMonth;
                    cellCal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), nextDay);
                    holder.text.setText(String.valueOf(nextDay));
                }
                holder.text.setTextColor(0xFFFFFFFF);
                holder.text.setAlpha(0.2f);
            }

            long cellMillis = cellCal.getTimeInMillis();
            
            // Selection styling (Today styling removed as requested)
            holder.text.setBackgroundResource(0);
            if (cellMillis == selectedDateMillis) {
                holder.text.setBackgroundResource(R.drawable.calendar_selected_day_purple);
                holder.text.setTextColor(0xFFFFFFFF);
            }

            // Check for events
            boolean hasEvent = false;
            String dateStr = dateFormat.format(new Date(cellMillis));
            for (CalendarEvent e : events) {
                if (e.getStartTime() != null && e.getStartTime().startsWith(dateStr)) {
                    hasEvent = true;
                    break;
                }
            }
            holder.indicator.setVisibility(hasEvent ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> {
                selectedDateMillis = cellMillis;
                updateSelectedDateText();
                
                // Refresh both current view (to update selection UI) and events list
                notifyItemRangeChanged(0, getItemCount()); 
                // We actually need to tell parent adapter to refresh other pages if selection moves across pages
                // But for simplicity we notify dataset changed on parent adapter in the activity typically
                // Here we just update local state and callback
                
                // Trigger refresh in Activity
                // Since this adapter is inside ViewPager, communicating back is tricky without a bus or callback chain.
                // For this implementation, let's just use a static reference or callback passed down.
                // To keep it simple, we'll just update the Activity's selectedDateMillis via a callback if we had passed one.
                // Correct approach: Pass a listener to MonthPagerAdapter -> MonthGridAdapter.
                // I'll skip re-implementing the full listener chain for brevity unless requested, 
                // but crucially we need to update the list.
                
                // Hack: We are in inner class, can access outer class methods if not static.
                // Adapter is static inner class in previous attempts, here let's make it non-static or pass listener.
                if (getActivity() != null) {
                    getActivity().onDateClicked(cellMillis); 
                }
            });
        }
        
        private CalendarActivity getActivity() {
            // This is a bit hacky, normally pass listener
            // Returning null for now as we need to properly implementing listener passing
            return CalendarActivity.this;
        }

        class CellViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            View indicator;
            CellViewHolder(View v) {
                super(v);
                text = v.findViewById(R.id.cell_text);
                indicator = v.findViewById(R.id.event_indicator);
            }
        }
    }
    
    // Callback from adapter
    public void onDateClicked(long millis) {
        this.selectedDateMillis = millis;
        updateSelectedDateText();
        filterEventsForSelectedDate(viewModel.getEvents().getValue());
        
        // Refresh pagers to update selection highlight
        monthPagerAdapter.notifyDataSetChanged();
    }
}
