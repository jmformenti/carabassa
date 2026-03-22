package org.atypical.carabassa.indexer.rdbms.entity.specification;

import org.apache.commons.lang3.time.DateUtils;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.SearchCondition;
import org.atypical.carabassa.core.model.SearchCriteria;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.atypical.carabassa.core.model.enums.PeriodType;
import org.atypical.carabassa.core.model.enums.SearchOperator;
import org.atypical.carabassa.core.model.enums.ValueType;
import org.atypical.carabassa.indexer.rdbms.entity.IndexedItemEntity;
import org.atypical.carabassa.indexer.rdbms.entity.IndexedItemEntity_;
import org.atypical.carabassa.indexer.rdbms.entity.TagEntity;
import org.atypical.carabassa.indexer.rdbms.entity.TagEntity_;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class ItemSpecification implements Specification<IndexedItemEntity> {

    private static final long serialVersionUID = -8307610449472579379L;

    private static final String ATTR_ID = "id";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_ON = "on";
    private static final String ATTR_FROM = "from";
    private static final String ATTR_TO = "to";
    private static final String ATTR_MISSING_TAG = "missing_tag";

    private static final String FULL_DATE = "yyyy-MM-dd";
    private static final String MONTH_DATE = "yyyy-MM";
    private static final String YEAR_DATE = "yyyy";

    private final Dataset dataset;
    private final SearchCriteria searchCriteria;
    private final Sort sort;

    public ItemSpecification(Dataset dataset, SearchCriteria searchCriteria, Sort sort) {
        this.dataset = dataset;
        this.searchCriteria = searchCriteria;
        this.sort = sort;
    }

    @Override
    public Predicate toPredicate(Root<IndexedItemEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

        final List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get(IndexedItemEntity_.DATASET), this.dataset));

        for (SearchCondition condition : searchCriteria.getConditions()) {
            predicates.add(toPredicateFromCondition(condition, root, query, builder));
        }

        if (sort != null && sort.isSorted() && query.getResultType() != Long.class
                && query.getResultType() != long.class) {
            List<Order> orders = new ArrayList<>();
            sort.forEach(order -> {
                List<String> entityProperties = java.util.Arrays.asList(
                        "id", "type", "filename", "format", "hash", "size", "creation", "modification", "archiveTime", "dataset");
                if (entityProperties.contains(order.getProperty())) {
                    if (order.isAscending()) {
                        orders.add(builder.asc(root.get(order.getProperty())));
                    } else {
                        orders.add(builder.desc(root.get(order.getProperty())));
                    }
                } else {
                    // Assume it's a tag name, like "duplicated.group"
                    Join<IndexedItemEntity, TagEntity> tagJoin = root.join(IndexedItemEntity_.TAGS, JoinType.LEFT);
                    tagJoin.on(builder.equal(tagJoin.get(TagEntity_.NAME), order.getProperty()));
                    if (order.isAscending()) {
                        orders.add(builder.asc(tagJoin.get(TagEntity_.TEXT_VALUE)));
                    } else {
                        orders.add(builder.desc(tagJoin.get(TagEntity_.TEXT_VALUE)));
                    }
                }
            });
            orders.add(builder.asc(root.get(IndexedItemEntity_.ID)));
            query.orderBy(orders);
        } else {
            query.orderBy(builder.asc(root.get(IndexedItemEntity_.ARCHIVE_TIME)),
                    builder.asc(root.get(IndexedItemEntity_.ID)));
        }

        return builder.and(predicates.toArray(new Predicate[0]));
    }

    private Predicate toPredicateFromCondition(SearchCondition condition, Root<IndexedItemEntity> root,
                                               CriteriaQuery<?> query, CriteriaBuilder builder) {

        if (condition.getOperation() == null) {
            return existsTagCondition(query, null, condition.getValue().toString(), builder, root);
        } else if (condition.getOperation() == SearchOperator.EQUAL) {
            Pair<Instant, Instant> periodDates;
            switch (condition.getKey()) {
                case ATTR_ID:
                    return builder.equal(root.get(IndexedItemEntity_.ID), condition.getValue().toString());
                case ATTR_TYPE:
                    return builder.equal(root.get(IndexedItemEntity_.TYPE),
                            ItemType.fromCode(condition.getValue().toString()));
                case ATTR_ON:
                    periodDates = getPeriodDates(condition.getValue().toString());
                    return builder.between(root.get(IndexedItemEntity_.ARCHIVE_TIME), periodDates.getFirst(),
                            periodDates.getSecond());
                case ATTR_FROM:
                    periodDates = getPeriodDates(condition.getValue().toString());
                    return builder.greaterThanOrEqualTo(root.get(IndexedItemEntity_.ARCHIVE_TIME),
                            periodDates.getFirst());
                case ATTR_TO:
                    periodDates = getPeriodDates(condition.getValue().toString());
                    return builder.lessThanOrEqualTo(root.get(IndexedItemEntity_.ARCHIVE_TIME),
                            periodDates.getSecond());
                case ATTR_MISSING_TAG:
                    Subquery<Long> subquery = itemsWithMissingTag(query, condition.getValue().toString(), builder);
                    return builder.not(root.get(IndexedItemEntity_.ID).in(subquery));
                default:
                    return existsTagCondition(query, condition.getKey(), condition.getValue().toString(), builder, root);
            }
        } else if (condition.getOperation() == SearchOperator.LESS_THAN ||
                   condition.getOperation() == SearchOperator.GREATER_THAN ||
                   condition.getOperation() == SearchOperator.LESS_THAN_EQUAL ||
                   condition.getOperation() == SearchOperator.GREATER_THAN_EQUAL) {
            if (ATTR_ID.equals(condition.getKey())) {
                if (condition.getOperation() == SearchOperator.LESS_THAN) {
                    return builder.lessThan(root.get(IndexedItemEntity_.ID), condition.getValue().toString());
                } else if (condition.getOperation() == SearchOperator.GREATER_THAN) {
                    return builder.greaterThan(root.get(IndexedItemEntity_.ID), condition.getValue().toString());
                } else if (condition.getOperation() == SearchOperator.LESS_THAN_EQUAL) {
                    return builder.lessThanOrEqualTo(root.get(IndexedItemEntity_.ID), condition.getValue().toString());
                } else if (condition.getOperation() == SearchOperator.GREATER_THAN_EQUAL) {
                    return builder.greaterThanOrEqualTo(root.get(IndexedItemEntity_.ID), condition.getValue().toString());
                }
            } else {
                return existsNumericTagCondition(query, condition.getKey(), condition.getValue().toString(), condition.getOperation(), builder, root);
            }
        }
        throw new IllegalArgumentException(String.format("Operation %s not implemented yet", condition.getOperation()));
    }

    private Subquery<Long> itemsWithMissingTag(CriteriaQuery<?> query, String tag, CriteriaBuilder builder) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<TagEntity> subRoot = subquery.from(TagEntity.class);
        subquery.where(resolveTagNamePredicate(builder, subRoot, tag));
        subquery.select(subRoot.get("itemId"));
        return subquery;
    }

    private Predicate existsTagCondition(CriteriaQuery<?> query, String tagName, String tagValue,
                                         CriteriaBuilder builder, Root<IndexedItemEntity> root) {
        return buildTagConditionSubquery(query, tagName, tagValue, null, builder, root);
    }

    private Predicate existsNumericTagCondition(CriteriaQuery<?> query, String tagName, String tagValue, SearchOperator op,
                                                CriteriaBuilder builder, Root<IndexedItemEntity> root) {
        return buildTagConditionSubquery(query, tagName, tagValue, op, builder, root);
    }

    private Predicate buildTagConditionSubquery(CriteriaQuery<?> query, String tagName, String tagValue, SearchOperator op,
                                                CriteriaBuilder builder, Root<IndexedItemEntity> root) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<TagEntity> subRoot = subquery.from(TagEntity.class);
        subquery.select(subRoot.get(TagEntity_.ITEM_ID));

        List<Predicate> subPredicates = new ArrayList<>();
        subPredicates.add(builder.equal(subRoot.get(TagEntity_.ITEM_ID), root.get(IndexedItemEntity_.ID)));

        if (tagName != null) {
            subPredicates.add(resolveTagNamePredicate(builder, subRoot, tagName));
        }

        if (tagValue != null) {
            if (op == null || op == SearchOperator.EQUAL) {
                subPredicates.add(builder.equal(builder.lower(subRoot.get(TagEntity_.TEXT_VALUE)),
                        builder.lower(builder.literal(tagValue))));
            } else {
                Double val;
                try {
                    val = Double.parseDouble(tagValue);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid numeric value for operator " + op + ": " + tagValue);
                }
                Predicate doublePredicate;
                Predicate longPredicate;
                switch(op) {
                    case GREATER_THAN:
                        doublePredicate = builder.greaterThan(subRoot.get(TagEntity_.DOUBLE_VALUE), val);
                        longPredicate = builder.greaterThan(subRoot.get(TagEntity_.LONG_VALUE), val.longValue());
                        break;
                    case LESS_THAN:
                        doublePredicate = builder.lessThan(subRoot.get(TagEntity_.DOUBLE_VALUE), val);
                        longPredicate = builder.lessThan(subRoot.get(TagEntity_.LONG_VALUE), val.longValue());
                        break;
                    case GREATER_THAN_EQUAL:
                        doublePredicate = builder.greaterThanOrEqualTo(subRoot.get(TagEntity_.DOUBLE_VALUE), val);
                        longPredicate = builder.greaterThanOrEqualTo(subRoot.get(TagEntity_.LONG_VALUE), val.longValue());
                        break;
                    case LESS_THAN_EQUAL:
                        doublePredicate = builder.lessThanOrEqualTo(subRoot.get(TagEntity_.DOUBLE_VALUE), val);
                        longPredicate = builder.lessThanOrEqualTo(subRoot.get(TagEntity_.LONG_VALUE), val.longValue());
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported operation for numeric tag search");
                }
                Predicate isDouble = builder.equal(subRoot.get(TagEntity_.VALUE_TYPE), ValueType.DOUBLE);
                Predicate isLong = builder.equal(subRoot.get(TagEntity_.VALUE_TYPE), ValueType.LONG);

                subPredicates.add(builder.or(
                    builder.and(isDouble, doublePredicate),
                    builder.and(isLong, longPredicate)
                ));
            }
        }

        subquery.where(subPredicates.toArray(new Predicate[0]));
        return builder.exists(subquery);
    }

    private Predicate resolveTagNamePredicate(CriteriaBuilder builder, Root<TagEntity> subRoot, String tagName) {
        return builder.equal(subRoot.get(TagEntity_.NAME), tagName);
    }

    private Pair<Instant, Instant> getPeriodDates(String value) {
        Instant startDate;
        try {
            startDate = dateIgnoringTimeZoneToInstant(DateUtils.parseDateStrictly(value, FULL_DATE));
            return PeriodType.DAY.getPeriodDates(startDate);
        } catch (ParseException e) {
            try {
                startDate = dateIgnoringTimeZoneToInstant(DateUtils.parseDateStrictly(value, MONTH_DATE));
                return PeriodType.MONTH.getPeriodDates(startDate);
            } catch (ParseException e1) {
                try {
                    startDate = dateIgnoringTimeZoneToInstant(DateUtils.parseDateStrictly(value, YEAR_DATE));
                    return PeriodType.YEAR.getPeriodDates(startDate);
                } catch (ParseException e2) {
                    throw new IllegalArgumentException(String.format("Error parsing date value %s", value));
                }
            }
        }
    }

    private Instant dateIgnoringTimeZoneToInstant(Date date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return LocalDateTime.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH), 0, 0).toInstant(ZoneOffset.UTC);
    }
}
