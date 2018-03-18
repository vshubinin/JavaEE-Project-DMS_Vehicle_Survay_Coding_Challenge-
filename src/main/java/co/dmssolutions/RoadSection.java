package co.dmssolutions;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.TreeMap;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;
import org.primefaces.model.chart.OhlcChartModel;
import org.primefaces.model.chart.OhlcChartSeries;


/**
 * Class holds stat data about road section where sensors are deployed<br>
 * Provides access to statistical data from sensors
 *
 * @author DMS Solutions
 */
public class RoadSection implements Serializable {

    public static final String SENSOR_A = "A";
    public static final String SENSOR_B = "B";
    public static final int DIRECTION_A = 1;
    public static final int DIRECTION_B = 2;

    private String _name;
    private ArrayList<Vehicle> _data;
    private int _speedLimit = 60;

    // Statistics releted vars
    TreeMap<Integer, ArrayList<Vehicle>> _carsByDayMap = new TreeMap<>();
    private SummaryStatistics _stats;
    private LineChartModel _carsByAllDaysChart;
    private LineChartModel _carsByDayChart;
    private LineChartModel _carsByDirectionOverAllDays;
    private OhlcChartModel _carsSpeedDistributionOverAllDays;
    private Integer _selectedDay = 0;


    // Constructors -------------------------------------------------------------------------------
    /**
     * Constructs new instance based on the provided sensor data
     *
     * @param data
     */
    public RoadSection(ArrayList<Vehicle> data) {
        _data = data;
        init();
    }

    // Methods ------------------------------------------------------------------------------------

    /**
     * Inits main statistics<br>
     * There are several approaches could be here: init statistic on demand, or init all statistics on data load
     */
    private void init() {
        ArrayList<Vehicle> tmpList;

        // Init general statistics
        _stats = new SummaryStatistics();

        int dayIndex = 0;
        long lastCheckedDate = -1;

        // Common loop which inits several statistics counters
        for (Vehicle vehicle : _data) {

            // General statistics model init
            _stats.addValue(vehicle.getSpeed());

            // Inits statistics on cars by day
            if (lastCheckedDate > vehicle.getTimeStamp().getTime()) {
                // check for sensor counter values drop in order to detect new day
                dayIndex++;
            }
            lastCheckedDate = vehicle.getTimeStamp().getTime();

            // Puts vehicle information over days
            if (_carsByDayMap.containsKey(dayIndex)) {
                tmpList = _carsByDayMap.get(dayIndex);
            } else {
                tmpList = new ArrayList<>();
            }
            tmpList.add(vehicle);
            _carsByDayMap.put(dayIndex, tmpList);
        }

        // Init graphs
        initCarsByAllDaysChart();
        initCarsByDayChart();
        initCarsByDirectionOverAllDays();
        initCarsSpeedDistributionOverAllDays();
    }


    /**
     * Prepares chart number of cars by days
     */
    private void initCarsByAllDaysChart() {
        _carsByAllDaysChart = new LineChartModel();
        _carsByAllDaysChart.setTitle("Total number of cars per day");
        _carsByAllDaysChart.setShowPointLabels(true);
        _carsByAllDaysChart.setAnimate(true);
        _carsByAllDaysChart.getAxes().put(AxisType.X, new CategoryAxis("Days"));
        Axis yAxis = _carsByAllDaysChart.getAxis(AxisType.Y);
        yAxis.setLabel("Nuber of cars");

        LineChartSeries series1 = new LineChartSeries();
        series1.setLabel("Cars per days");
        int day = 1;
        for (Integer keyName : _carsByDayMap.keySet()) {
            series1.set("Day " + day++, _carsByDayMap.get(keyName).size());
        }
        _carsByAllDaysChart.addSeries(series1);
    }


    /**
     * Prepares chart number of cars by days
     */
    private void initCarsByDayChart() {
        _carsByDayChart = new LineChartModel();
        _carsByDayChart.setTitle("Cars per hour of the day");
        //_carsByDayChart.setShowPointLabels(true);
        _carsByDayChart.setLegendPosition("e");
        _carsByDayChart.getAxes().put(AxisType.X, new CategoryAxis("Hour of the day"));
        Axis yAxis = _carsByDayChart.getAxis(AxisType.Y);
        yAxis.setLabel("Nuber of cars");

        // _carsByDayMap contains day index as a key, iterate via day indexes
        for (int n = 0; n < _carsByDayMap.keySet().size(); n++) {
            LineChartSeries series = new LineChartSeries();
            series.setLabel("Day " + (n + 1)); // index is 0 based, make it 1 based

            SimpleDateFormat sdf = new SimpleDateFormat("HH:'00'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            TreeMap<String, Integer> carsByHourOfTheDay = new TreeMap<>();

            for (Vehicle vehicle : _carsByDayMap.get(n)) {
                String key = sdf.format(vehicle.getTimeStamp());
                if (carsByHourOfTheDay.containsKey(key)) {
                    carsByHourOfTheDay.put(key, (carsByHourOfTheDay.get(key) + 1));
                } else {
                    carsByHourOfTheDay.put(key, 1);
                }
            }

            for (String key : carsByHourOfTheDay.keySet()) {
                series.set(key, carsByHourOfTheDay.get(key));
            }
            _carsByDayChart.addSeries(series);
        }
    }


    /**
     * Prepares chart number of cars by days
     */
    private void initCarsByDirectionOverAllDays() {
        _carsByDirectionOverAllDays = new LineChartModel();
        _carsByDirectionOverAllDays.setTitle("Cars per hour per direction over all days (avarage)");
        _carsByDirectionOverAllDays.setShowPointLabels(true);
        _carsByDirectionOverAllDays.setLegendPosition("e");
        _carsByDirectionOverAllDays.getAxes().put(AxisType.X, new CategoryAxis("Hour of the day"));
        Axis yAxis = _carsByDirectionOverAllDays.getAxis(AxisType.Y);
        yAxis.setLabel("Nuber of cars");

        LineChartSeries series1 = new LineChartSeries();
        series1.setLabel("Direction A");
        LineChartSeries series2 = new LineChartSeries();
        series2.setLabel("Direction B");

        SimpleDateFormat sdf = new SimpleDateFormat("HH:'00'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        TreeMap<String, Integer> dirA = new TreeMap<>();
        TreeMap<String, Integer> dirB = new TreeMap<>();

        for (Vehicle vehicle : _data) {
            String key = sdf.format(vehicle.getTimeStamp());
            if (vehicle.getDirection() == DIRECTION_A) {
                if (dirA.containsKey(key)) {
                    dirA.put(key, (dirA.get(key) + 1));
                } else {
                    dirA.put(key, 1);
                }
            } else if (vehicle.getDirection() == DIRECTION_B) {
                if (dirB.containsKey(key)) {
                    dirB.put(key, (dirB.get(key) + 1));
                } else {
                    dirB.put(key, 1);
                }
            }
        }

        for (String key : dirA.keySet()) {
            series1.set(key, dirA.get(key));
        }

        for (String key : dirB.keySet()) {
            series2.set(key, dirB.get(key));
        }

        _carsByDirectionOverAllDays.addSeries(series1);
        _carsByDirectionOverAllDays.addSeries(series2);
    }


    /**
     * Prepares graph data of cars speed distribution over all days
     */
    private void initCarsSpeedDistributionOverAllDays() {
        _carsSpeedDistributionOverAllDays = new OhlcChartModel();
        _carsSpeedDistributionOverAllDays.setTitle("Speed (mean, min, max) per hour over all days");
        _carsSpeedDistributionOverAllDays.setCandleStick(true);
        _carsSpeedDistributionOverAllDays.getAxis(AxisType.Y).setLabel("Speed (km/h)");

        Axis axisX = _carsSpeedDistributionOverAllDays.getAxis(AxisType.X);
        axisX.setLabel("Hour of the day");
        axisX.setMin(-0.5);
        axisX.setMax(23.5);
        axisX.setTickInterval("1");
//        DateAxis axis = new DateAxis("Hour of the day");
//        axis.setTickAngle(-50);
//        axis.setMin("1970-01-01-00-00-00");
//        axis.setMax("1970-01-01-23-59-59");
//        axis.setTickFormat("%H-%#M");
        _carsSpeedDistributionOverAllDays.getAxes().put(AxisType.X, axisX);

        SimpleDateFormat sdf = new SimpleDateFormat("HH");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        TreeMap<String, SummaryStatistics> speedDistrib = new TreeMap<>();

        for (Vehicle vehicle : _data) {
            String key = sdf.format(vehicle.getTimeStamp());
            if (speedDistrib.containsKey(key)) {
                speedDistrib.get(key).addValue(vehicle.getSpeed());
            } else {
                SummaryStatistics stat = new SummaryStatistics();
                stat.addValue(vehicle.getSpeed());
                speedDistrib.put(key, stat);
            }
        }

        for (String key : speedDistrib.keySet()) {
            _carsSpeedDistributionOverAllDays.add(
                    new OhlcChartSeries(key,
                            speedDistrib.get(key).getMean(),
                            speedDistrib.get(key).getMax(),
                            speedDistrib.get(key).getMin(),
                            speedDistrib.get(key).getMean()));
        }
    }

// Getters & Setters --------------------------------------------------------------------------

    public int getSpeedLimit() {
        return _speedLimit;
    }


    public void setSpeedLimit(int speedLimit) {
        _speedLimit = speedLimit;
    }


    public ArrayList<Vehicle> getData() {
        return _data;
    }


    public void setData(ArrayList<Vehicle> data) {
        _data = data;
        init();
    }


    public String getName() {
        return _name;
    }


    public void setName(String name) {
        _name = name;
    }


    public SummaryStatistics getStats() {
        return _stats;
    }


    public LineChartModel getCarsByAllDaysChart() {
        return _carsByAllDaysChart;
    }


    public LineChartModel getCarsByDayChart() {
        return _carsByDayChart;
    }


    public LineChartModel getCarsByDirectionOverAllDays() {
        return _carsByDirectionOverAllDays;
    }


    public OhlcChartModel getCarsSpeedDistributionOverAllDays() {
        return _carsSpeedDistributionOverAllDays;
    }


    public int getTotalDays() {
        return _carsByDayMap.keySet().size();
    }


    public Integer getSelectedDay() {
        return _selectedDay;
    }


    public void setSelectedDay(Integer selectedDay) {
        _selectedDay = selectedDay;
    }

}