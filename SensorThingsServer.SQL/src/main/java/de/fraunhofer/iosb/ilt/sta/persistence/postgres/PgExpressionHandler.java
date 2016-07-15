/*
 * Copyright (C) 2016 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.sta.persistence.postgres;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Constant;
import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Visitor;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpression;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.TimeTemplate;
import com.querydsl.spatial.GeometryExpression;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQuery;
import de.fraunhofer.iosb.ilt.sta.path.EntityProperty;
import de.fraunhofer.iosb.ilt.sta.path.NavigationProperty;
import de.fraunhofer.iosb.ilt.sta.path.Property;
import de.fraunhofer.iosb.ilt.sta.query.OrderBy;
import de.fraunhofer.iosb.ilt.sta.query.expression.ExpressionVisitor;
import de.fraunhofer.iosb.ilt.sta.query.expression.Path;
import de.fraunhofer.iosb.ilt.sta.query.expression.constant.BooleanConstant;
import de.fraunhofer.iosb.ilt.sta.query.expression.constant.DateConstant;
import de.fraunhofer.iosb.ilt.sta.query.expression.constant.DateTimeConstant;
import de.fraunhofer.iosb.ilt.sta.query.expression.constant.DoubleConstant;
import de.fraunhofer.iosb.ilt.sta.query.expression.constant.DurationConstant;
import de.fraunhofer.iosb.ilt.sta.query.expression.constant.IntegerConstant;
import de.fraunhofer.iosb.ilt.sta.query.expression.constant.LineStringConstant;
import de.fraunhofer.iosb.ilt.sta.query.expression.constant.PointConstant;
import de.fraunhofer.iosb.ilt.sta.query.expression.constant.PolygonConstant;
import de.fraunhofer.iosb.ilt.sta.query.expression.constant.StringConstant;
import de.fraunhofer.iosb.ilt.sta.query.expression.constant.TimeConstant;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.arithmetic.Add;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.arithmetic.Divide;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.arithmetic.Modulo;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.arithmetic.Multiply;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.arithmetic.Subtract;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.comparison.Equal;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.comparison.GreaterEqual;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.comparison.GreaterThan;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.comparison.LessEqual;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.comparison.LessThan;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.comparison.NotEqual;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.date.Date;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.date.Day;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.date.FractionalSeconds;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.date.Hour;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.date.MaxDateTime;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.date.MinDateTime;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.date.Minute;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.date.Month;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.date.Now;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.date.Second;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.date.Time;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.date.TotalOffsetMinutes;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.date.Year;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.geospatial.GeoDistance;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.geospatial.GeoIntersects;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.geospatial.GeoLength;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.logical.And;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.logical.Not;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.logical.Or;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.math.Ceiling;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.math.Floor;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.math.Round;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.spatialrelation.STContains;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.spatialrelation.STCrosses;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.spatialrelation.STDisjoint;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.spatialrelation.STEquals;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.spatialrelation.STIntersects;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.spatialrelation.STOverlaps;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.spatialrelation.STRelate;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.spatialrelation.STTouches;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.spatialrelation.STWithin;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.string.Concat;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.string.EndsWith;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.string.IndexOf;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.string.Length;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.string.StartsWith;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.string.Substring;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.string.SubstringOf;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.string.ToLower;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.string.ToUpper;
import de.fraunhofer.iosb.ilt.sta.query.expression.function.string.Trim;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.annotation.Nullable;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkt;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Hylke van der Schaaf
 */
public class PgExpressionHandler implements ExpressionVisitor<Expression<?>> {

    public static class ConstantStringExpression extends StringExpression {

        public ConstantStringExpression(final String constant) {
            super(ConstantImpl.create(constant));
        }

        @Override
        @Nullable
        public <R, C> R accept(Visitor<R, C> v, C context) {
            return v.visit((Constant<String>) mixin, context);
        }
    }

    public static class ConstantGeometryExpression extends GeometryExpression {

        public ConstantGeometryExpression(final Geometry constant) {
            super(ConstantImpl.create(constant));
        }

        @Override
        public Object accept(Visitor v, Object context) {
            return v.visit((Constant<Geometry>) mixin, context);
        }
    }

    public static class ConstantNumberExpression<N extends Number & Comparable<?>> extends NumberExpression<N> {

        private static final long serialVersionUID = 1L;

        public ConstantNumberExpression(final N constant) {
            super(ConstantImpl.create(constant));
        }

        @Override
        @Nullable
        public <R, C> R accept(final Visitor<R, C> v, @Nullable final C context) {
            return v.visit((Constant<N>) mixin, context);
        }

    }

    public static class ConstantDateTimeExpression extends DateTimeExpression<Timestamp> {

        public ConstantDateTimeExpression(final Timestamp ts) {
            super(ConstantImpl.create(ts));
        }

        @Override
        @Nullable
        public <R, C> R accept(Visitor<R, C> v, C context) {
            return v.visit((Constant<Timestamp>) mixin, context);
        }

    }

    public static class ConstantDateExpression extends DateTimeExpression<java.sql.Date> {

        public ConstantDateExpression(final java.sql.Date ts) {
            super(ConstantImpl.create(ts));
        }

        @Override
        @Nullable
        public <R, C> R accept(Visitor<R, C> v, C context) {
            return v.visit((Constant<java.sql.Date>) mixin, context);
        }

    }

    public static class ConstantTimeExpression extends DateTimeExpression<java.sql.Time> {

        public ConstantTimeExpression(final java.sql.Time ts) {
            super(ConstantImpl.create(ts));
        }

        @Override
        @Nullable
        public <R, C> R accept(Visitor<R, C> v, C context) {
            return v.visit((Constant<java.sql.Time>) mixin, context);
        }

    }

    /**
     * Some paths, like Observation.result and the time-interval paths, return
     * two column references. This class is just to encapsulate these cases. If
     * this Expression is used as a normal Expression, the first of the two will
     * be used.
     */
    public static class ListExpression implements Expression {

        private final Collection<Expression<?>> expressions;

        public ListExpression(Collection<Expression<?>> expressions) {
            this.expressions = expressions;
        }

        public Collection<Expression<?>> getExpressions() {
            return expressions;
        }

        @Override
        public Object accept(Visitor v, Object context) {
            return expressions.iterator().next().accept(v, context);
        }

        @Override
        public Class getType() {
            return expressions.iterator().next().getType();
        }

    }
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PgExpressionHandler.class);
    private final PathSqlBuilder psb;
    /**
     * The table reference for the main table of the request.
     */
    private final PathSqlBuilder.TableRef tableRef;

    public PgExpressionHandler(PathSqlBuilder psb, PathSqlBuilder.TableRef tableRef) {
        this.psb = psb;
        this.tableRef = tableRef;
    }

    public void addFilterToQuery(de.fraunhofer.iosb.ilt.sta.query.expression.Expression filter, SQLQuery<Tuple> sqlQuery) {
        Expression<?> filterExpression = filter.accept(this);
        if (filterExpression instanceof Predicate) {
            Predicate predicate = (Predicate) filterExpression;
            sqlQuery.where(predicate);
        } else {
            LOGGER.error("Filter is not a predicate but a {}.", filterExpression.getClass().getName());
            throw new IllegalArgumentException("Filter is not a predicate but a " + filterExpression.getClass().getName());
        }
    }

    public void addOrderbyToQuery(OrderBy orderBy, SQLQuery<Tuple> sqlQuery) {
        Expression<?> resultExpression = orderBy.getExpression().accept(this);
        if (resultExpression instanceof PgExpressionHandler.ListExpression) {
            for (Expression<?> sqlExpression : ((PgExpressionHandler.ListExpression) resultExpression).getExpressions()) {
                addToQuery(orderBy, sqlExpression, sqlQuery);
            }
        } else {
            addToQuery(orderBy, resultExpression, sqlQuery);
        }
    }

    private void addToQuery(OrderBy orderBy, Expression<?> sqlExpression, SQLQuery<Tuple> sqlQuery) {
        if (sqlExpression instanceof ComparableExpressionBase) {
            ComparableExpressionBase comparable = (ComparableExpressionBase) sqlExpression;
            if (orderBy.getType() == OrderBy.OrderType.Ascending) {
                sqlQuery.orderBy(comparable.asc());
            } else {
                sqlQuery.orderBy(comparable.desc());
            }
        }
    }

    private <T extends Expression<?>> T checkType(Class<T> expectedClazz, Expression<?> input) {
        if (expectedClazz.isAssignableFrom(input.getClass())) {
            LOGGER.info("Is {}: {} ({} -- {})", expectedClazz.getName(), input, input.getClass().getName(), input.getType().getName());
            return expectedClazz.cast(input);
        } else {
            LOGGER.info("Not a {}: {} ({} -- {})", expectedClazz.getName(), input, input.getClass().getName(), input.getType().getName());
            throw new IllegalArgumentException("Could not convert parameter of type " + input.getClass().getName() + " to type " + expectedClazz.getName());
        }
    }

    private <T extends Expression<?>> T getSingleOfType(Class<T> expectedClazz, Expression<?> input) {
        if (input instanceof ListExpression) {
            ListExpression listExpression = (ListExpression) input;
            for (Expression<?> subResult : listExpression.getExpressions()) {
                try {
                    return checkType(expectedClazz, subResult);
                } catch (IllegalArgumentException e) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.info("Parameter not of correct type.", e);
                    }
                }
            }
            throw new IllegalArgumentException("Non of the entries could be converted to type " + expectedClazz.getName());
        } else {
            return checkType(expectedClazz, input);
        }
    }

    @Override
    public Expression<?> visit(Path path) {
        PathSqlBuilder.TableRef pathTableRef = tableRef.copy();
        List<Property> elements = path.getElements();
        int pathLength = elements.size();
        for (int i = 0; i < pathLength; i++) {
            Property element = elements.get(i);
            if (element instanceof EntityProperty) {
                EntityProperty entityProperty = (EntityProperty) element;
                Map<String, Expression<?>> pathExpressions = PropertyResolver.expressionsForProperty(entityProperty, pathTableRef.qPath, new LinkedHashMap<>());
                if (pathExpressions.size() == 1) {
                    return pathExpressions.values().iterator().next();
                } else {
                    return getSubExpression(elements, i, pathExpressions);
                }
            } else if (element instanceof NavigationProperty) {
                NavigationProperty navigationProperty = (NavigationProperty) element;
                psb.queryEntityType(navigationProperty.getType(), null, pathTableRef);
            }
        }
        throw new IllegalArgumentException("Path does not end in an EntityProperty: " + path);
    }

    private Expression<?> getSubExpression(List<Property> elements, int curIdx, Map<String, Expression<?>> pathExpressions) {
        int nextIdx = curIdx + 1;
        if (elements.size() > nextIdx) {
            Property subProperty = elements.get(nextIdx);
            return pathExpressions.get(subProperty.getName());
        } else {
            return new ListExpression(pathExpressions.values());
        }
    }

    @Override
    public Expression<?> visit(BooleanConstant node) {
        return node.getValue() ? Expressions.TRUE : Expressions.FALSE;
    }

    @Override
    public Expression<?> visit(DateConstant node) {
        LocalDate date = node.getValue();
        Calendar instance = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        instance.set(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth());
        ConstantDateExpression constant = new ConstantDateExpression(new java.sql.Date(instance.getTimeInMillis()));
        return constant;
    }

    @Override
    public Expression<?> visit(DateTimeConstant node) {
        ConstantDateTimeExpression constant = new ConstantDateTimeExpression(new Timestamp(node.getValue().getMillis()));
        return constant;
    }

    @Override
    public Expression<?> visit(DoubleConstant node) {
        ConstantNumberExpression constant = new ConstantNumberExpression(node.getValue());
        return constant;
    }

    @Override
    public Expression<?> visit(DurationConstant node) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Expression<?> visit(IntegerConstant node) {
        ConstantNumberExpression constant = new ConstantNumberExpression(node.getValue());
        return constant;
    }

    @Override
    public Expression<?> visit(LineStringConstant node) {
        Geometry geom = Wkt.fromWkt(node.getSource());
        return new ConstantGeometryExpression(geom);
    }

    @Override
    public Expression<?> visit(PointConstant node) {
        Geometry geom = Wkt.fromWkt(node.getSource());
        return new ConstantGeometryExpression(geom);
    }

    @Override
    public Expression<?> visit(PolygonConstant node) {
        Geometry geom = Wkt.fromWkt(node.getSource());
        return new ConstantGeometryExpression(geom);
    }

    @Override
    public Expression<?> visit(StringConstant node) {
        ConstantStringExpression constant = new ConstantStringExpression(node.getValue());
        return constant;
    }

    @Override
    public Expression<?> visit(TimeConstant node) {
        LocalTime time = node.getValue();
        Calendar instance = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        instance.set(1970, 1, 1, time.getHourOfDay(), time.getMinuteOfHour(), time.getSecondOfMinute());
        ConstantTimeExpression constant = new ConstantTimeExpression(new java.sql.Time(instance.getTimeInMillis()));
        return constant;
    }

    @Override
    public Expression<?> visit(Add node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        Expression<?> p2 = params.get(1).accept(this);
        NumberExpression n1 = getSingleOfType(NumberExpression.class, p1);
        NumberExpression n2 = getSingleOfType(NumberExpression.class, p2);
        return n1.add(n2);
    }

    @Override
    public Expression<?> visit(Divide node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        Expression<?> p2 = params.get(1).accept(this);
        NumberExpression n1 = getSingleOfType(NumberExpression.class, p1);
        NumberExpression n2 = getSingleOfType(NumberExpression.class, p2);
        return n1.divide(n2);
    }

    @Override
    public Expression<?> visit(Modulo node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        Expression<?> p2 = params.get(1).accept(this);
        NumberExpression n1 = getSingleOfType(NumberExpression.class, p1);
        NumberExpression n2 = getSingleOfType(NumberExpression.class, p2);
        return n1.castToNum(BigDecimal.class).mod(n2.castToNum(BigDecimal.class));
    }

    @Override
    public Expression<?> visit(Multiply node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        Expression<?> p2 = params.get(1).accept(this);
        NumberExpression n1 = getSingleOfType(NumberExpression.class, p1);
        NumberExpression n2 = getSingleOfType(NumberExpression.class, p2);
        return n1.multiply(n2);
    }

    @Override
    public Expression<?> visit(Subtract node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        Expression<?> p2 = params.get(1).accept(this);
        NumberExpression n1 = getSingleOfType(NumberExpression.class, p1);
        NumberExpression n2 = getSingleOfType(NumberExpression.class, p2);
        return n1.subtract(n2);
    }

    @Override
    public Expression<?> visit(Equal node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        Expression<?> p2 = params.get(1).accept(this);
        ComparableExpressionBase c1 = getSingleOfType(ComparableExpressionBase.class, p1);
        ComparableExpressionBase c2 = getSingleOfType(ComparableExpressionBase.class, p2);
        return c1.eq(c2);
    }

    @Override
    public Expression<?> visit(GreaterEqual node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        Expression<?> p2 = params.get(1).accept(this);
        try {
            ComparableExpression c1 = getSingleOfType(ComparableExpression.class, p1);
            ComparableExpression c2 = getSingleOfType(ComparableExpression.class, p2);
            return c1.goe(c2);
        } catch (IllegalArgumentException e) {
            NumberExpression c1 = getSingleOfType(NumberExpression.class, p1);
            NumberExpression c2 = getSingleOfType(NumberExpression.class, p2);
            return c1.goe(c2);
        }
    }

    @Override
    public Expression<?> visit(GreaterThan node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        Expression<?> p2 = params.get(1).accept(this);
        try {
            ComparableExpression c1 = getSingleOfType(ComparableExpression.class, p1);
            ComparableExpression c2 = getSingleOfType(ComparableExpression.class, p2);
            return c1.gt(c2);
        } catch (IllegalArgumentException e) {
            NumberExpression c1 = getSingleOfType(NumberExpression.class, p1);
            NumberExpression c2 = getSingleOfType(NumberExpression.class, p2);
            return c1.gt(c2);
        }
    }

    @Override
    public Expression<?> visit(LessEqual node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        Expression<?> p2 = params.get(1).accept(this);
        try {
            ComparableExpression c1 = getSingleOfType(ComparableExpression.class, p1);
            ComparableExpression c2 = getSingleOfType(ComparableExpression.class, p2);
            return c1.loe(c2);
        } catch (IllegalArgumentException e) {
            NumberExpression c1 = getSingleOfType(NumberExpression.class, p1);
            NumberExpression c2 = getSingleOfType(NumberExpression.class, p2);
            return c1.loe(c2);
        }
    }

    @Override
    public Expression<?> visit(LessThan node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        Expression<?> p2 = params.get(1).accept(this);
        try {
            ComparableExpression c1 = getSingleOfType(ComparableExpression.class, p1);
            ComparableExpression c2 = getSingleOfType(ComparableExpression.class, p2);
            return c1.lt(c2);
        } catch (IllegalArgumentException e) {
            NumberExpression c1 = getSingleOfType(NumberExpression.class, p1);
            NumberExpression c2 = getSingleOfType(NumberExpression.class, p2);
            return c1.lt(c2);
        }
    }

    @Override
    public Expression<?> visit(NotEqual node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        Expression<?> p2 = params.get(1).accept(this);
        try {
            ComparableExpression c1 = getSingleOfType(ComparableExpression.class, p1);
            ComparableExpression c2 = getSingleOfType(ComparableExpression.class, p2);
            return c1.ne(c2);
        } catch (IllegalArgumentException e) {
            NumberExpression c1 = getSingleOfType(NumberExpression.class, p1);
            NumberExpression c2 = getSingleOfType(NumberExpression.class, p2);
            return c1.ne(c2);
        }
    }

    @Override
    public Expression<?> visit(Date node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression param = node.getParameters().get(0);
        Expression<?> input = param.accept(this);
        DateTimeExpression inExp = getSingleOfType(DateTimeExpression.class, input);
        DateExpression date = SQLExpressions.date(inExp);
        return date;
    }

    @Override
    public Expression<?> visit(Day node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression param = node.getParameters().get(0);
        Expression<?> input = param.accept(this);
        DateTimeExpression inExp = getSingleOfType(DateTimeExpression.class, input);
        return inExp.dayOfMonth();
    }

    @Override
    public Expression<?> visit(FractionalSeconds node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression param = node.getParameters().get(0);
        Expression<?> input = param.accept(this);
        DateTimeExpression inExp = getSingleOfType(DateTimeExpression.class, input);
        return inExp.milliSecond();
    }

    @Override
    public Expression<?> visit(Hour node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression param = node.getParameters().get(0);
        Expression<?> input = param.accept(this);
        DateTimeExpression inExp = getSingleOfType(DateTimeExpression.class, input);
        return inExp.hour();
    }

    @Override
    public Expression<?> visit(MaxDateTime node) {
        return new ConstantDateTimeExpression(new Timestamp(PostgresPersistenceManager.DATETIME_MAX.getMillis()));
    }

    @Override
    public Expression<?> visit(MinDateTime node) {
        return new ConstantDateTimeExpression(new Timestamp(PostgresPersistenceManager.DATETIME_MIN.getMillis()));
    }

    @Override
    public Expression<?> visit(Minute node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression param = node.getParameters().get(0);
        Expression<?> input = param.accept(this);
        DateTimeExpression inExp = getSingleOfType(DateTimeExpression.class, input);
        return inExp.minute();
    }

    @Override
    public Expression<?> visit(Month node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression param = node.getParameters().get(0);
        Expression<?> input = param.accept(this);
        DateTimeExpression inExp = getSingleOfType(DateTimeExpression.class, input);
        return inExp.month();
    }

    @Override
    public Expression<?> visit(Now node) {
        return DateTimeExpression.currentTimestamp();
    }

    @Override
    public Expression<?> visit(Second node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression param = node.getParameters().get(0);
        Expression<?> input = param.accept(this);
        DateTimeExpression inExp = getSingleOfType(DateTimeExpression.class, input);
        return inExp.second();
    }

    @Override
    public Expression<?> visit(Time node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression param = node.getParameters().get(0);
        Expression<?> input = param.accept(this);
        DateTimeExpression inExp = getSingleOfType(DateTimeExpression.class, input);
        TimeTemplate<java.sql.Time> time = Expressions.timeTemplate(java.sql.Time.class, "pg_catalog.time({0})", inExp);
        // TODO: Throw exception if node is a constant and not in UTC.
        return time;
    }

    @Override
    public Expression<?> visit(TotalOffsetMinutes node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression param = node.getParameters().get(0);
        Expression<?> input = param.accept(this);
        DateTimeExpression inExp = getSingleOfType(DateTimeExpression.class, input);
        NumberExpression<Integer> offset = Expressions.numberTemplate(Integer.class, "timezone({0})", inExp).divide(60);
        return offset;
    }

    @Override
    public Expression<?> visit(Year node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression param = node.getParameters().get(0);
        Expression<?> input = param.accept(this);
        DateTimeExpression inExp = getSingleOfType(DateTimeExpression.class, input);
        return inExp.year();
    }

    @Override
    public Expression<?> visit(GeoDistance node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        GeometryExpression g1 = getSingleOfType(GeometryExpression.class, e1);
        GeometryExpression g2 = getSingleOfType(GeometryExpression.class, e2);
        return g1.distance(g2);
    }

    @Override
    public Expression<?> visit(GeoIntersects node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        GeometryExpression g1 = getSingleOfType(GeometryExpression.class, e1);
        GeometryExpression g2 = getSingleOfType(GeometryExpression.class, e2);
        return g1.intersects(g2);
    }

    @Override
    public Expression<?> visit(GeoLength node) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Expression<?> visit(And node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        Expression<?> p2 = params.get(1).accept(this);
        BooleanExpression b1 = getSingleOfType(BooleanExpression.class, p1);
        BooleanExpression b2 = getSingleOfType(BooleanExpression.class, p2);
        return b1.and(b2);
    }

    @Override
    public Expression<?> visit(Not node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        BooleanExpression b1 = getSingleOfType(BooleanExpression.class, p1);
        return b1.not();
    }

    @Override
    public Expression<?> visit(Or node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        Expression<?> p2 = params.get(1).accept(this);
        BooleanExpression b1 = getSingleOfType(BooleanExpression.class, p1);
        BooleanExpression b2 = getSingleOfType(BooleanExpression.class, p2);
        return b1.or(b2);
    }

    @Override
    public Expression<?> visit(Ceiling node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        NumberExpression n1 = getSingleOfType(NumberExpression.class, p1);
        return n1.ceil();
    }

    @Override
    public Expression<?> visit(Floor node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        NumberExpression n1 = getSingleOfType(NumberExpression.class, p1);
        return n1.floor();
    }

    @Override
    public Expression<?> visit(Round node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        Expression<?> p1 = params.get(0).accept(this);
        NumberExpression n1 = getSingleOfType(NumberExpression.class, p1);
        return n1.round();
    }

    @Override
    public Expression<?> visit(STContains node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        GeometryExpression g1 = getSingleOfType(GeometryExpression.class, e1);
        GeometryExpression g2 = getSingleOfType(GeometryExpression.class, e2);
        return g1.contains(g2);
    }

    @Override
    public Expression<?> visit(STCrosses node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        GeometryExpression g1 = getSingleOfType(GeometryExpression.class, e1);
        GeometryExpression g2 = getSingleOfType(GeometryExpression.class, e2);
        return g1.crosses(g2);
    }

    @Override
    public Expression<?> visit(STDisjoint node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        GeometryExpression g1 = getSingleOfType(GeometryExpression.class, e1);
        GeometryExpression g2 = getSingleOfType(GeometryExpression.class, e2);
        return g1.disjoint(g2);
    }

    @Override
    public Expression<?> visit(STEquals node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        GeometryExpression g1 = getSingleOfType(GeometryExpression.class, e1);
        GeometryExpression g2 = getSingleOfType(GeometryExpression.class, e2);
        return g1.eq(g2);
    }

    @Override
    public Expression<?> visit(STIntersects node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        GeometryExpression g1 = getSingleOfType(GeometryExpression.class, e1);
        GeometryExpression g2 = getSingleOfType(GeometryExpression.class, e2);
        return g1.intersects(g2);
    }

    @Override
    public Expression<?> visit(STOverlaps node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        GeometryExpression g1 = getSingleOfType(GeometryExpression.class, e1);
        GeometryExpression g2 = getSingleOfType(GeometryExpression.class, e2);
        return g1.overlaps(g2);
    }

    @Override
    public Expression<?> visit(STRelate node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p3 = node.getParameters().get(2);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        GeometryExpression g1 = getSingleOfType(GeometryExpression.class, e1);
        GeometryExpression g2 = getSingleOfType(GeometryExpression.class, e2);
        if (p3 instanceof StringConstant) {
            StringConstant e3 = (StringConstant) p3;
            String s3 = e3.getValue();
            return g1.relate(g2, s3);
        }
        throw new IllegalArgumentException("ST_RELATE can only be used with a string constant as third parameter.");
    }

    @Override
    public Expression<?> visit(STTouches node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        GeometryExpression g1 = getSingleOfType(GeometryExpression.class, e1);
        GeometryExpression g2 = getSingleOfType(GeometryExpression.class, e2);
        return g1.touches(g2);
    }

    @Override
    public Expression<?> visit(STWithin node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        GeometryExpression g1 = getSingleOfType(GeometryExpression.class, e1);
        GeometryExpression g2 = getSingleOfType(GeometryExpression.class, e2);
        return g1.within(g2);
    }

    @Override
    public Expression<?> visit(Concat node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        StringExpression s1 = getSingleOfType(StringExpression.class, e1);
        StringExpression s2 = getSingleOfType(StringExpression.class, e2);
        return s1.concat(s2);
    }

    @Override
    public Expression<?> visit(EndsWith node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        StringExpression s1 = getSingleOfType(StringExpression.class, e1);
        StringExpression s2 = getSingleOfType(StringExpression.class, e2);
        return s1.endsWith(s2);
    }

    @Override
    public Expression<?> visit(IndexOf node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        StringExpression s1 = getSingleOfType(StringExpression.class, e1);
        StringExpression s2 = getSingleOfType(StringExpression.class, e2);
        return s1.indexOf(s2);
    }

    @Override
    public Expression<?> visit(Length node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression param = node.getParameters().get(0);
        Expression<?> input = param.accept(this);
        StringExpression inExp = getSingleOfType(StringExpression.class, input);
        return inExp.length();
    }

    @Override
    public Expression<?> visit(StartsWith node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        StringExpression s1 = getSingleOfType(StringExpression.class, e1);
        StringExpression s2 = getSingleOfType(StringExpression.class, e2);
        return s1.startsWith(s2);
    }

    @Override
    public Expression<?> visit(Substring node) {
        List<de.fraunhofer.iosb.ilt.sta.query.expression.Expression> params = node.getParameters();
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        StringExpression s1 = getSingleOfType(StringExpression.class, e1);
        NumberExpression n2 = getSingleOfType(NumberExpression.class, e2);
        if (params.size() > 2) {
            de.fraunhofer.iosb.ilt.sta.query.expression.Expression p3 = node.getParameters().get(2);
            Expression<?> e3 = p3.accept(this);
            NumberExpression n3 = getSingleOfType(NumberExpression.class, e3);
            return s1.substring(n2, n3);
        }
        return s1.substring(n2);
    }

    @Override
    public Expression<?> visit(SubstringOf node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p1 = node.getParameters().get(0);
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression p2 = node.getParameters().get(1);
        Expression<?> e1 = p1.accept(this);
        Expression<?> e2 = p2.accept(this);
        StringExpression s1 = getSingleOfType(StringExpression.class, e1);
        StringExpression s2 = getSingleOfType(StringExpression.class, e2);
        return s2.contains(s1);
    }

    @Override
    public Expression<?> visit(ToLower node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression param = node.getParameters().get(0);
        Expression<?> input = param.accept(this);
        StringExpression inExp = getSingleOfType(StringExpression.class, input);
        return inExp.toLowerCase();
    }

    @Override
    public Expression<?> visit(ToUpper node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression param = node.getParameters().get(0);
        Expression<?> input = param.accept(this);
        StringExpression inExp = getSingleOfType(StringExpression.class, input);
        return inExp.toUpperCase();
    }

    @Override
    public Expression<?> visit(Trim node) {
        de.fraunhofer.iosb.ilt.sta.query.expression.Expression param = node.getParameters().get(0);
        Expression<?> input = param.accept(this);
        StringExpression inExp = getSingleOfType(StringExpression.class, input);
        return inExp.trim();
    }
}