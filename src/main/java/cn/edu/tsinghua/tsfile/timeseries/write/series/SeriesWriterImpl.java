package cn.edu.tsinghua.tsfile.timeseries.write.series;

import java.io.IOException;
import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.edu.tsinghua.tsfile.common.conf.TSFileDescriptor;
import cn.edu.tsinghua.tsfile.common.utils.Binary;
import cn.edu.tsinghua.tsfile.file.metadata.enums.TSDataType;
import cn.edu.tsinghua.tsfile.file.metadata.statistics.Statistics;
import cn.edu.tsinghua.tsfile.timeseries.write.desc.MeasurementDescriptor;
import cn.edu.tsinghua.tsfile.timeseries.write.exception.PageException;
import cn.edu.tsinghua.tsfile.timeseries.write.io.TsFileIOWriter;
import cn.edu.tsinghua.tsfile.timeseries.write.page.IPageWriter;

/**
 * A implementation of {@code ISeriesWriter}. {@code SeriesWriterImpl} consists
 * of a {@code PageWriter}, a {@code ValueWriter}, and two {@code Statistics}.
 *
 * @author kangrong
 * @see ISeriesWriter ISeriesWriter
 */
public class SeriesWriterImpl implements ISeriesWriter {
    private static final Logger LOG = LoggerFactory.getLogger(SeriesWriterImpl.class);

    // initial value for this.valueCountForNextSizeCheck
    private static final int MINIMUM_RECORD_COUNT_FOR_CHECK = 1;

    private final TSDataType dataType;
    /**
     * help to encode data of this series
     */
    private final IPageWriter pageWriter;
    /**
     * page size threshold
     */
    private final long psThres;
    private final int pageCountUpperBound;
    /**
     * value writer to encode data
     */
    private ValueWriter dataValueWriter;

    /**
     * value count on of a page. It will be reset after calling
     * {@code writePage()}
     */
    private int valueCount;
    private int valueCountForNextSizeCheck;
    /**
     * statistic on a page. It will be reset after calling {@code writePage()}
     */
    private Statistics<?> pageStatistics;
    /**
     * statistic on a stage. It will be reset after calling {@code writeToFileWriter()}
     */
    private Statistics<?> seriesStatistics;
    // time of the latest written time value pair
    private long time;
    private long minTimestamp = -1;
    private String deltaObjectId;
    private MeasurementDescriptor desc;

    public SeriesWriterImpl(String deltaObjectId, MeasurementDescriptor desc, IPageWriter pageWriter,
                            int pageSizeThreshold) {
        this.deltaObjectId = deltaObjectId;
        this.desc = desc;
        this.dataType = desc.getType();
        this.pageWriter = pageWriter;
        this.psThres = pageSizeThreshold;

        // initial check of memory usage. So that we have enough data to make an initial prediction
        this.valueCountForNextSizeCheck = MINIMUM_RECORD_COUNT_FOR_CHECK;

        // init statistics for this series and page
        this.seriesStatistics = Statistics.getStatsByType(dataType);
        resetPageStatistics();

        this.dataValueWriter = new ValueWriter();
        this.pageCountUpperBound = TSFileDescriptor.getInstance().getConfig().maxNumberOfPointsInPage;

        this.dataValueWriter.setTimeEncoder(desc.getTimeEncoder());
        this.dataValueWriter.setValueEncoder(desc.getValueEncoder());
    }

    /**
     * reset statistics of page by dataType of this measurement
     */
    private void resetPageStatistics() {
        this.pageStatistics = Statistics.getStatsByType(dataType);
    }

    @Override
    public void write(long time, long value) throws IOException {
        this.time = time;
        ++valueCount;
        dataValueWriter.write(time, value);
        pageStatistics.updateStats(value);
        if (minTimestamp == -1)
            minTimestamp = time;
        checkPageSize();
    }

    @Override
    public void write(long time, int value) throws IOException {
        this.time = time;
        ++valueCount;
        dataValueWriter.write(time, value);
        pageStatistics.updateStats(value);
        if (minTimestamp == -1)
            minTimestamp = time;
        checkPageSize();
    }

    @Override
    public void write(long time, boolean value) throws IOException {
        this.time = time;
        ++valueCount;
        dataValueWriter.write(time, value);
        pageStatistics.updateStats(value);
        if (minTimestamp == -1)
            minTimestamp = time;
        checkPageSize();
    }

    @Override
    public void write(long time, float value) throws IOException {
        this.time = time;
        ++valueCount;
        dataValueWriter.write(time, value);
        pageStatistics.updateStats(value);
        if (minTimestamp == -1)
            minTimestamp = time;
        checkPageSize();
    }

    @Override
    public void write(long time, double value) throws IOException {
        this.time = time;
        ++valueCount;
        dataValueWriter.write(time, value);
        pageStatistics.updateStats(value);
        if (minTimestamp == -1)
            minTimestamp = time;
        checkPageSize();
    }

    @Override
    public void write(long time, BigDecimal value) throws IOException {
        this.time = time;
        ++valueCount;
        dataValueWriter.write(time, value);
        pageStatistics.updateStats(value);
        if (minTimestamp == -1)
            minTimestamp = time;
        checkPageSize();
    }

    @Override
    public void write(long time, Binary value) throws IOException {
        this.time = time;
        ++valueCount;
        dataValueWriter.write(time, value);
        pageStatistics.updateStats(value);
        if (minTimestamp == -1)
            minTimestamp = time;
        checkPageSize();
    }

    /**
     * check occupied memory size, if it exceeds the PageSize threshold, flush
     * them to given OutputStream.
     */
    private void checkPageSize() {
        if (valueCount == pageCountUpperBound) {    // data points num exceeds threshold
            LOG.debug("current line count reaches the upper bound, write page {}", desc);
            writePage();
        } else if (valueCount >= valueCountForNextSizeCheck) {  // need to check memory size
            // not checking the memory used for every value
            long currentColumnSize = dataValueWriter.estimateMaxMemSize();
            if (currentColumnSize > psThres) {  // memory size exceeds threshold
                // we will write the current page
                LOG.debug("enough size, write page {}", desc);
                writePage();
            } else {
                LOG.debug("{}:{} not enough size, now: {}, change to {}", deltaObjectId, desc, valueCount,
                        valueCountForNextSizeCheck);
            }
            // reset the valueCountForNextSizeCheck for the next page
            valueCountForNextSizeCheck = (int) (((float) psThres / currentColumnSize) * valueCount);
        }
    }

    /**
     * flush data into {@code IPageWriter}
     */
    private void writePage() {
        try {
            pageWriter.writePage(dataValueWriter.getBytes(), valueCount, pageStatistics, time, minTimestamp);
            // update statistics of this series
            this.seriesStatistics.mergeStatistics(this.pageStatistics);
        } catch (IOException e) {
            LOG.error("meet error in dataValueWriter.getBytes(),ignore this page, {}", e.getMessage());
        } catch (PageException e) {
            LOG.error("meet error in pageWriter.writePage,ignore this page, error message:{}", e.getMessage());
        } finally {
            // clear start time stamp for next initializing
            minTimestamp = -1;
            valueCount = 0;
            dataValueWriter.reset();
            resetPageStatistics();
        }
    }

    @Override
    public void writeToFileWriter(TsFileIOWriter tsfileWriter) throws IOException {
        if (valueCount > 0) {   // flush data in memory
            writePage();
        }
        pageWriter.writeToFileWriter(tsfileWriter, seriesStatistics);
        pageWriter.reset();
        // reset series_statistics
        this.seriesStatistics = Statistics.getStatsByType(dataType);
    }

    @Override
    public long estimateMaxSeriesMemSize() {
        return dataValueWriter.estimateMaxMemSize() + pageWriter.estimateMaxPageMemSize();
    }
}
