# Renewable Energy Plant System (REPS)  
A Scala-based renewable energy data analysis and monitoring system, integrated with Fingrid OpenAPI, supporting wind/solar/hydro power generation data collection, analysis, and visualization.  

### Core Modules  
- **Historical Data Analysis**  
  - Query generation data by time range  
  - Generate interactive HTML charts  
  - Statistical metric calculations (mean/median/mode)  
  - CSV export (supports hourly/daily/weekly/monthly grouping)  

- **Real-time Monitoring**  
  - Set generation alert thresholds  
  - Colored terminal alerts  
  - Manual refresh of latest data  

- **Equipment Control Simulation**  
  - Select energy device type  
  - Input target power  
  - Animated operation progress demonstration  

### Features  
- Functional programming implementation  
- Tail recursion optimization for control flow  
- ANSI-colored terminal output  
- Automatic timestamped file generation  

### Typical Workflow Examples  
**Historical Data Analysis**  
1. Select energy type (1-3)  
2. Input time range (format: dd/MM/yyyy HH:mm)  
3. Choose chart generation/stats/export  

**Real-time Monitoring**  
1. Set alert threshold (unit: MW)  
2. Input `y` to refresh data, `n` to return to main menu  

**Equipment Control**  
1. Select target device  
2. Input power value  
3. View simulation animation  

### File Naming Convention  
- Chart files: `[EnergyType]_[Timestamp].html`  
- Data files: `[EnergyType]_[Timestamp].csv`  
