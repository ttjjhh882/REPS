Renewable Energy Plant System (REPS)
A Scala-based system for data analysis and monitoring of renewable energy, integrated with the Fingrid Open API. Supports data collection, analysis, and visualization for wind, solar, and hydro power generation.

Core Modules
Historical Data Analysis

Query generation data by time range

Generate interactive HTML charts

Statistical metrics calculation (mean/median/mode, etc.)

CSV export (supports grouping by hour/day/week/month)

Real-Time Monitoring

Set generation threshold alerts

Color-coded terminal alert display

Manual refresh of latest data

Device Control Simulation

Select energy device type

Input target power value

Simulated operation animation display

Implemented using pure functional programming

Tail recursion optimized control flow

ANSI-colored terminal output

Auto-generates timestamped files

Example Workflow
Historical Data Analysis

Select energy type (1–3)

Enter time range (format: dd/MM/yyyy HH:mm)

Choose to generate chart/statistics/export

Real-Time Monitoring

Set alert threshold (unit: MW)

Enter y to refresh data, n to return to main menu

Device Control

Select target device

Enter power value

View simulated operation animation

Chart file: [EnergyType]_[Timestamp].html
Data file: [EnergyType]_[Timestamp].csv
