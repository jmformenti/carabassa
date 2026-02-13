package org.atypical.carabassa.indexer.rdbms.entity.specification;

import org.apache.commons.lang3.time.DateUtils;
import org.atypical.carabassa.core.component.tagger.impl.ImageMetadataTagger;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.SearchCondition;
import org.atypical.carabassa.core.model.SearchCriteria;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.atypical.carabassa.core.model.enums.PeriodType;
import org.atypical.carabassa.core.model.enums.SearchOperator;
import org.atypical.carabassa.indexer.rdbms.entity.IndexedItemEntity;
import org.atypical.carabassa.indexer.rdbms.entity.IndexedItemEntity_;
import org.atypical.carabassa.indexer.rdbms.entity.TagEntity;
import org.atypical.carabassa.indexer.rdbms.entity.TagEntity_;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

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
    private static final String ATTR_CITY = "city";

    private static final String FULL_DATE = "yyyy-MM-dd";
    private static final String MONTH_DATE = "yyyy-MM";
    private static final String YEAR_DATE = "yyyy";

    private final Dataset dataset;
    private final SearchCriteria searchCriteria;

    public ItemSpecification(Dataset dataset, SearchCriteria searchCriteria) {
        this.dataset = dataset;
        this.searchCriteria = searchCriteria;
    }

    @Override
    public Predicate toPredicate(Root<IndexedItemEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

        final List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get(IndexedItemEntity_.DATASET), this.dataset));

        for (SearchCondition condition : searchCriteria.getConditions()) {
            predicates.add(toPredicateFromCondition(condition, root, query, builder));
        }

        query.orderBy(builder.asc(root.get(IndexedItemEntity_.ARCHIVE_TIME)),
                builder.asc(root.get(IndexedItemEntity_.ID)));

        return builder.and(predicates.toArray(new Predicate[0]));
    }

    private Predicate toPredicateFromCondition(SearchCondition condition, Root<IndexedItemEntity> root,
                                               CriteriaQuery<?> query, CriteriaBuilder builder) {

        if (condition.getOperation() == null) {
            Join<IndexedItemEntity, TagEntity> tags = addTagsJoin(root, query);
            return builder.like(builder.lower(tags.get(TagEntity_.TEXT_VALUE)),
                    builder.lower(builder.literal("%" + condition.getValue() + "%")));
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
                    return builder.greaterThanOrEqualTo(root.get(IndexedItemEntity_.ARCHIVE_TIME), periodDates.getFirst());
                case ATTR_TO:
                    periodDates = getPeriodDates(condition.getValue().toString());
                    return builder.lessThanOrEqualTo(root.get(IndexedItemEntity_.ARCHIVE_TIME), periodDates.getSecond());
                case ATTR_CITY: {
                    Join<IndexedItemEntity, TagEntity> tags = addTagsJoin(root, query);
                    return builder.and(builder.equal(tags.get(TagEntity_.NAME), ImageMetadataTagger.TAG_CITY),
                            builder.like(builder.lower(tags.get(TagEntity_.TEXT_VALUE)),
                                    builder.lower(builder.literal("%" + condition.getValue() + "%"))));
                }
                default:
                    Join<IndexedItemEntity, TagEntity> tags = addTagsJoin(root, query);
                    return builder.and(builder.equal(builder.lower(tags.get(TagEntity_.NAME)),
                                    builder.lower(builder.literal(condition.getKey()))),
                            builder.equal(builder.lower(tags.get(TagEntity_.TEXT_VALUE)),
                                    builder.lower(builder.literal(condition.getValue().toString()))));
            }
        } else if (condition.getOperation() == SearchOperator.LESS_THAN && ATTR_ID.equals(condition.getKey())) {
            return builder.lessThan(root.get(IndexedItemEntity_.ID), condition.getValue().toString());
        } else if (condition.getOperation() == SearchOperator.GREATER_THAN && ATTR_ID.equals(condition.getKey())) {
            return builder.greaterThan(root.get(IndexedItemEntity_.ID), condition.getValue().toString());
        }
        throw new IllegalArgumentException(String.format("Operation %s not implemented yet", condition.getOperation()));
    }

    private Join<IndexedItemEntity, TagEntity> addTagsJoin(Root<IndexedItemEntity> root, CriteriaQuery<?> query) {
        query.distinct(true);
        return root.join(IndexedItemEntity_.TAGS, JoinType.INNER);
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
