package cn.edu.tsinghua.tsfile.timeseries.filter.operator;

import cn.edu.tsinghua.tsfile.timeseries.filter.basic.UnaryFilter;
import cn.edu.tsinghua.tsfile.timeseries.filter.factory.FilterType;
import cn.edu.tsinghua.tsfile.timeseries.filter.visitor.AbstractFilterVisitor;
import cn.edu.tsinghua.tsfile.timeseries.filter.visitor.TimeValuePairFilterVisitor;
import cn.edu.tsinghua.tsfile.timeseries.read.datatype.TimeValuePair;

/**
 * Equals
 *
 * @param <T> comparable data type
 * @author CGF
 */
public class Eq<T extends Comparable<T>> extends UnaryFilter<T> {

    private static final long serialVersionUID = -6668083116644568248L;

    public Eq(T value, FilterType filterType) {
        super(value, filterType);
    }

    @Override
    public <R> R accept(AbstractFilterVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return getFilterType() + " == " + value;
    }

    @Override
    public <R> R accept(TimeValuePair value, TimeValuePairFilterVisitor<R> visitor) {
        return visitor.visit(value, this);
    }
}