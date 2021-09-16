/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.custom.scalar;

import graphql.language.StringValue;
import graphql.schema.*;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Created on Nov, 2020 by @author bobo
 */
public abstract class DateTimeScalar {

    private static final String DEFAULT_NAME = "DateTime";

    public static GraphQLScalarType build() {
        return GraphQLScalarType.newScalar()
                .name(DEFAULT_NAME)
                .description("DateTime type")
                .coercing(new Coercing<Date, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof Date) {
                            return DateTimeHelper.toISOString((Date)dataFetcherResult);
                        } else {
                            Date result = convertImpl(dataFetcherResult);
                            if (result == null) {
                                throw new CoercingSerializeException("Invalid value '" + dataFetcherResult + "' for Date");
                            } else {
                                return DateTimeHelper.toISOString(result);
                            }
                        }
                    }

                    @Override
                    public Date parseValue(Object input) throws CoercingParseValueException {
                        Date result = convertImpl(input);
                        if (result == null) {
                            throw new CoercingParseValueException("Invalid value '" + input + "' for Date");
                        } else {
                            return result;
                        }
                    }

                    @Override
                    public Date parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (!(input instanceof StringValue)) {
                            return null;
                        } else {
                            String value = ((StringValue)input).getValue();
                            Date result = convertImpl(value);
                            return result;
                        }
                    }
                })
                .build();
    }

    private static Date convertImpl(Object input) {
        if (input instanceof String) {
            LocalDateTime localDateTime = DateTimeHelper.parseDate((String)input);
            if (localDateTime != null) {
                return DateTimeHelper.toDate(localDateTime);
            }
        }

        return null;
    }

}
