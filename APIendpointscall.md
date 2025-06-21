Spring Boot Attendance System API Usage
This Postman collection provides complete examples for interacting with the unified DailyCalendar attendance system API.
Environment Setup
BASE_URL=http://localhost:8080 
Basic Calendar Operations
Get a Particular Day Status
GET  {{BASE_URL}}/api/calendar/{year}/{month}/day/{day}/status
Get All Calendars
GET {{BASE_URL}}/api/calendar/all
Get Calendar by Year and Month
GET {{BASE_URL}}/api/calendar/2025/01
Create New Calendar
POST {{BASE_URL}}/api/calendar Content-Type: application/json  {   "year": "2025",   "month": "01",   "days": [     {       "day": 1,       "type": "holiday",       "attendance": null     },     {       "day": 2,       "type": "working",       "attendance": "wfoffice"     },     {       "day": 3,       "type": "working",       "attendance": "wfh"     },     {       "day": 4,       "type": "weekend",       "attendance": null     },     {       "day": 5,       "type": "weekend",       "attendance": null     }   ] } 
Update Calendar
PUT {{BASE_URL}}/api/calendar/2025/01 Content-Type: application/json  {   "days": [     {       "day": 1,       "type": "holiday",       "attendance": null     },     {       "day": 2,       "type": "working",       "attendance": "wfoffice"     },     {       "day": 3,       "type": "working",       "attendance": "wfh"     },     {       "day": 4,       "type": "weekend",       "attendance": null     },     {       "day": 5,       "type": "weekend",       "attendance": null     }   ] } 
Delete Calendar
DELETE {{BASE_URL}}/api/calendar/2025/01 
Generate Calendar
POST {{BASE_URL}}/api/calendar/generate/2025/02 Content-Type: application/json  [1, 15, 26] 
Day Management
Update a Particular Day Type
PUT {{BASE_URL}}/api/calendar/2025/01/day/3/type?type=holiday 
Add Day Entry
POST {{BASE_URL}}/api/calendar/2025/01/day Content-Type: application/json  {   "day": 6,   "type": "working",   "attendance": "wfoffice" } 
Attendance Management
Update Attendance
PUT {{BASE_URL}}/api/calendar/2025/01/day/2/attendance?attendance=wfh 
Clear Attendance
DELETE {{BASE_URL}}/api/calendar/2025/01/day/3/attendance 
Bulk Update Attendance
PUT {{BASE_URL}}/api/calendar/2025/01/bulk-attendance Content-Type: application/json  {   "2": "wfoffice",   "3": "wfh",   "6": "wfoffice",   "7": "wfoffice",   "8": "wfh" } 
Bulk Update Day Types
PUT {{BASE_URL}}/api/calendar/2025/01/bulk-day-types Content-Type: application/json  {   "9": "holiday",   "10": "working",   "11": "working",   "12": "working" } 
Statistics
Get Calendar Statistics
GET {{BASE_URL}}/api/calendar/2025/01/stats 
Get Overall Statistics
GET {{BASE_URL}}/api/calendar/stats/overall 
Get Yearly Statistics
GET {{BASE_URL}}/api/calendar/stats/year/2025 
Query Endpoints
Get Calendars by Year
GET {{BASE_URL}}/api/calendar/year/2025 
Get Calendars with Holidays
GET {{BASE_URL}}/api/calendar/holidays 
Get Calendars with Attendance
GET {{BASE_URL}}/api/calendar/attendance 
Get Calendars with Office Attendance
GET {{BASE_URL}}/api/calendar/attendance/office 
Get Calendars with WFH Attendance
GET {{BASE_URL}}/api/calendar/attendance/wfh 
Get Calendars with Mixed Attendance
GET {{BASE_URL}}/api/calendar/attendance/mixed
Get Calendars with Incomplete Attendance
GET {{BASE_URL}}/api/calendar/attendance/incomplete 
Get Calendars with Full Attendance
GET {{BASE_URL}}/api/calendar/attendance/full 
Get Calendars by Date Range
GET {{BASE_URL}}/api/calendar/range?startYear=2025&startMonth=01&endYear=2025&endMonth=03 
Utility Endpoints
Check if Calendar Exists
GET {{BASE_URL}}/api/calendar/exists/2025/01
Get Total Calendars Count
GET {{BASE_URL}}/api/calendar/count
Get Distinct Years
GET {{BASE_URL}}/api/calendar/years
Get Calendar Count by Year
GET {{BASE_URL}}/api/calendar/year/2025/count