package com.attendance.system.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "calendar_data")
@CompoundIndex(def = "{'year': 1, 'month': 1}", unique = true)
public class DailyCalendar {

    @Id
    private String id;

    @NotBlank(message = "Year is required")
    @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits")
    @Indexed
    private String year;

    @NotBlank(message = "Month is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12")
    @Indexed
    private String month;

    @NotNull(message = "Days list is required")
    private List<DayEntry> days;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Constructor with parameters
    public DailyCalendar(String year, String month) {
        this.year = year;
        this.month = month;
        this.days = new ArrayList<>();
    }

    public DailyCalendar(String year, String month, List<DayEntry> days) {
        this.year = year;
        this.month = month;
        this.days = days != null ? days : new ArrayList<>();
    }

    // Inner class for Day Entry
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayEntry {

        @Min(value = 1, message = "Day must be between 1 and 31")
        @Max(value = 31, message = "Day must be between 1 and 31")
        private int day; // 1 - 31

        @NotBlank(message = "Type is required")
        @Pattern(regexp = "^(working|holiday|weekend)$", message = "Type must be working, holiday, or weekend")
        private String type; // "working", "holiday", "weekend"

        @Pattern(regexp = "^(wfoffice|wfh)$", message = "Attendance must be wfoffice or wfh")
        private String attendance; // "wfoffice", "wfh", or null

        @Override
        public String toString() {
            return "DayEntry{" +
                    "day=" + day +
                    ", type='" + type + '\'' +
                    ", attendance='" + attendance + '\'' +
                    '}';
        }
    }

    // ===== UTILITY METHODS =====

    public DayEntry getDayEntry(int day) {
        return days.stream()
                .filter(d -> d.getDay() == day)
                .findFirst()
                .orElse(null);
    }

    public void addDayEntry(DayEntry dayEntry) {
        // Remove existing entry for the same day if exists
        days.removeIf(d -> d.getDay() == dayEntry.getDay());
        days.add(dayEntry);
    }

    public void updateAttendance(int day, String attendance) {
        DayEntry dayEntry = getDayEntry(day);
        if (dayEntry != null && "working".equals(dayEntry.getType())) {
            dayEntry.setAttendance(attendance);
        }
    }

    public void updateDayType(int day, String type) {
        DayEntry dayEntry = getDayEntry(day);
        if (dayEntry != null) {
            dayEntry.setType(type);
            // Clear attendance for non-working days
            if (!"working".equals(type)) {
                dayEntry.setAttendance(null);
            }
        }
    }

    // ===== STATISTICAL METHODS =====

    public long getWorkingDaysCount() {
        return days.stream()
                .filter(d -> "working".equals(d.getType()))
                .count();
    }

    public long getHolidaysCount() {
        return days.stream()
                .filter(d -> "holiday".equals(d.getType()))
                .count();
    }

    public long getWeekendsCount() {
        return days.stream()
                .filter(d -> "weekend".equals(d.getType()))
                .count();
    }

    public long getOfficeAttendanceCount() {
        return days.stream()
                .filter(d -> "wfoffice".equals(d.getAttendance()))
                .count();
    }

    public long getWfhAttendanceCount() {
        return days.stream()
                .filter(d -> "wfh".equals(d.getAttendance()))
                .count();
    }

    public long getAttendanceDaysCount() {
        return days.stream()
                .filter(d -> d.getAttendance() != null)
                .count();
    }

    public long getWorkingDaysWithoutAttendance() {
        return days.stream()
                .filter(d -> "working".equals(d.getType()) && d.getAttendance() == null)
                .count();
    }

    public double getAttendanceRate() {
        long workingDays = getWorkingDaysCount();
        if (workingDays == 0) return 0.0;
        long attendedDays = getAttendanceDaysCount();
        return (double) attendedDays / workingDays;
    }

    public double getOfficeAttendanceRate() {
        long workingDays = getWorkingDaysCount();
        if (workingDays == 0) return 0.0;
        long officeDays = getOfficeAttendanceCount();
        return (double) officeDays / workingDays;
    }

    public double getWfhAttendanceRate() {
        long workingDays = getWorkingDaysCount();
        if (workingDays == 0) return 0.0;
        long wfhDays = getWfhAttendanceCount();
        return (double) wfhDays / workingDays;
    }

    // ===== VALIDATION METHODS =====

    public boolean isValidDay(int day) {
        return day >= 1 && day <= 31;
    }

    public boolean hasDay(int day) {
        return getDayEntry(day) != null;
    }

    public boolean isWorkingDay(int day) {
        DayEntry entry = getDayEntry(day);
        return entry != null && "working".equals(entry.getType());
    }

    public boolean hasAttendanceForDay(int day) {
        DayEntry entry = getDayEntry(day);
        return entry != null && entry.getAttendance() != null;
    }

    @Override
    public String toString() {
        return "DailyCalendar{" +
                "id='" + id + '\'' +
                ", year='" + year + '\'' +
                ", month='" + month + '\'' +
                ", days=" + days +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}