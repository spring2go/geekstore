/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.data_import.parser;

import io.geekstore.common.utils.NormalizeUtil;
import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Validates and parses CSV files into a data structure which can then be used to created new entities.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Component
@Slf4j
public class ImportParser {

    private List<String> requiredColumns = Arrays.asList(
            "name",
            "slug",
            "description",
            "assets",
            "facets",
            "optionGroups",
            "optionValues",
            "sku",
            "price",
            "variantAssets",
            "variantFacets"
    );

    public ParseResult<ParsedProductWithVariants> parseProducts(Reader reader) {

        // 参考：
        // https://www.baeldung.com/opencsv
        CSVReader csvReader = new CSVReader(reader);
//        CSVParser parser = new CSVParserBuilder()
//                .withIgnoreLeadingWhiteSpace(true)
//                .build();
//        CSVReader csvReader = new CSVReaderBuilder(reader)
//                .withCSVParser(parser)
//                .build();

        try {
            List<String[]> records = csvReader.readAll();
            return this.processRawRecords(records);
        } catch (Exception e) {
            List<String> errors = new ArrayList<>();
            errors.add(e.toString());
            ParseResult parseResult = new ParseResult();
            parseResult.setErrors(errors);
            parseResult.setProcessed(0);
            return parseResult;
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                log.error("Fail to close reader", ex);
            }
        }
    }

    private ParseResult<ParsedProductWithVariants> processRawRecords(List<String[]> records) {
        ParseResult<ParsedProductWithVariants> parseResult = new ParseResult<>();

        ParsedProductWithVariants currentRow = null;
        String[] headerRow = records.get(0);
        List<String[]> rest = records.subList(1, records.size());
        int totalProducts = (int) rest.stream().map(row -> row[0])
                .filter(name -> {
                    if (name == null) return false;
                    return !StringUtils.isEmpty(name.trim());
                }).count();
        String columnError = validateRequiredColumns(headerRow);
        if (columnError != null) {
            parseResult.getErrors().add(columnError);
            parseResult.setProcessed(0);
            return parseResult;
        }
        int line = 1;
        for(String[] record : rest) {
            line++;
            String columnCountError = validateColumnCount(headerRow, record);
            if (columnCountError != null) {
                parseResult.getErrors().add(columnCountError + " on line " + line);
                continue;
            }
            RawProductRecord r = mapRowToObject(headerRow, record);
            if (!StringUtils.isEmpty(r.getName())) {
                if (currentRow != null) {
                    populateOptionGroupValues(currentRow);
                    parseResult.getResults().add(currentRow);
                }
                currentRow = new ParsedProductWithVariants();
                currentRow.setProduct(parseProductFromRecord(r));
                currentRow.getVariants().add(parseVariantFromRecord(r));
            } else {
                if (currentRow != null) {
                    ParsedProductVariant parsedProductVariant = parseVariantFromRecord(r);
                    currentRow.getVariants().add(parsedProductVariant);
                }
            }
            String optionError = validateOptionValueCount(r, currentRow);
            if(optionError != null) {
                parseResult.getErrors().add(optionError + " on line " + line);
            }
        }
        if (currentRow != null) {
            populateOptionGroupValues(currentRow);
            parseResult.getResults().add(currentRow);
        }

        parseResult.setProcessed(totalProducts);
        return parseResult;
    }

    private void populateOptionGroupValues(ParsedProductWithVariants currentRow) {
        List<List<String>> values = currentRow.getVariants()
                .stream().map(v -> v.getOptionValues()).collect(Collectors.toList());
        for(int i = 0; i < currentRow.getProduct().getOptionGroups().size(); i++) {
            StringOptionGroup og = currentRow.getProduct().getOptionGroups().get(i);
            final int index = i;
            List<String> ogValues = values.stream().map(v -> v.get(index)).distinct().collect(Collectors.toList());
            og.setValues(ogValues);
        }
    }

    private String validateRequiredColumns(String[] r) {
        List<String> rowKeys = Arrays.asList(r);
        List<String> missing = new ArrayList<>();
        for(String col : requiredColumns) {
            if (!rowKeys.contains(col)) {
                missing.add(col);
            }
        }
        if (!CollectionUtils.isEmpty(missing)) {
            String error = missing.stream().map(m -> "\"" + m + "\"").collect(Collectors.joining(", "));
            return "The import file is missing the following columns: " + error;
        }
        return null;
    }

    private String validateColumnCount(String[] columns, String[] row) {
        if (columns.length != row.length) {
            return "Invalid Record Length: header length is " + columns.length + ", got " + row.length;
        }
        return null;
    }

    private RawProductRecord mapRowToObject(String[] columns, String[] row) {
        Map<String, String> objMap = new HashMap<>();
        for(int i = 0;  i < row.length; i++) {
            objMap.put(columns[i], row[i]);
        }
        RawProductRecord rawProductRecord = new RawProductRecord();
        rawProductRecord.setName(objMap.get("name"));
        rawProductRecord.setSlug(objMap.get("slug"));
        rawProductRecord.setDescription(objMap.get("description"));
        rawProductRecord.setAssets(objMap.get("assets"));
        rawProductRecord.setFacets(objMap.get("facets"));
        rawProductRecord.setOptionGroups(objMap.get("optionGroups"));
        rawProductRecord.setOptionValues(objMap.get("optionValues"));
        rawProductRecord.setSku(objMap.get("sku"));
        rawProductRecord.setPrice(objMap.get("price"));
        rawProductRecord.setSockOnHand(objMap.get("stockOnHand"));
        rawProductRecord.setTrackInventory(objMap.get("trackInventory"));
        rawProductRecord.setVariantAssets(objMap.get("variantAssets"));
        rawProductRecord.setVariantFacets(objMap.get("variantFacets"));
        return rawProductRecord;
    }

    private String validateOptionValueCount(RawProductRecord r, ParsedProductWithVariants currentRow) {
        if (currentRow == null) return null;
        List<String> optionValues = parseStringArray(r.getOptionValues());
        if (currentRow.getProduct().getOptionGroups().size() != optionValues.size()) {
            return "The number of optionValues must match the number of optionGroups";
        }
        return null;
    }

    private ParsedProduct parseProductFromRecord(RawProductRecord r) {
        String name = parseString(r.getName());
        String slug = parseString(r.getSlug());
        if (StringUtils.isEmpty(slug)) {
            slug = NormalizeUtil.normalizeString(name, "-");
        }
        ParsedProduct parsedProduct = new ParsedProduct();
        parsedProduct.setName(name);
        parsedProduct.setSlug(slug);
        parsedProduct.setDescription(parseString(r.getDescription()));
        parsedProduct.setAssetPaths(parseStringArray(r.getAssets()));
        parsedProduct.setOptionGroups(
                parseStringArray(r.getOptionGroups()).stream().map(ogName -> {
                    StringOptionGroup stringOptionGroup = new StringOptionGroup();
                    stringOptionGroup.setName(ogName);
                    return stringOptionGroup;
                }).collect(Collectors.toList()));
        parsedProduct.setFacets(
                parseStringArray(r.getFacets()).stream().map(pair -> {
                    String[] stringArray = pair.split(":");
                    StringFacet stringFacet = new StringFacet();
                    stringFacet.setFacet(stringArray[0]);
                    stringFacet.setValue(stringArray[1]);
                    return stringFacet;
                }).collect(Collectors.toList()));
        return parsedProduct;
    }

    private ParsedProductVariant parseVariantFromRecord(RawProductRecord r) {
        ParsedProductVariant parsedProductVariant = new ParsedProductVariant();
        parsedProductVariant.setOptionValues(parseStringArray(r.getOptionValues()));
        parsedProductVariant.setSku(parseString(r.getSku()));
        parsedProductVariant.setPrice(parseFloat(r.getPrice()));
        parsedProductVariant.setStockOnHand(parseInteger(r.getSockOnHand()));
        parsedProductVariant.setTrackInventory(parseBoolean(r.getTrackInventory()));
        parsedProductVariant.setAssetPaths(parseStringArray(r.getVariantAssets()));
        parsedProductVariant.setFacets(
                parseStringArray(r.getVariantFacets()).stream().map(
                        pair -> {
                            String[] stringArray = pair.split(":");
                            StringFacet stringFacet = new StringFacet();
                            stringFacet.setFacet(stringArray[0]);
                            stringFacet.setValue(stringArray[1]);
                            return stringFacet;
                        }).collect(Collectors.toList()));
        return parsedProductVariant;
    }

    private String parseString(String input) {
        if (input == null) return "";
        return input.trim();
    }

    private Integer parseInteger(String input) {
        if (StringUtils.isEmpty(input)) return 0;
        return Integer.parseInt(input);
    }

    private Float parseFloat(String input) {
        if (StringUtils.isEmpty(input)) return 0.0F;
        return Float.parseFloat(input);
    }

    private boolean parseBoolean(String input) {
        if (input == null) return false;
        switch (input.toLowerCase()) {
            case "true":
            case "1":
            case "yes":
                return true;
            default:
                return false;
        }
    }

    private List<String> parseStringArray(String input) {
        return parseStringArray(input, "\\|");
    }

    private List<String> parseStringArray(String input, String separator) {
        if (StringUtils.isEmpty(input)) return new ArrayList<>();
        return Arrays.stream(input.trim().split(separator)).map(s -> s.trim())
                .filter(s -> !StringUtils.isEmpty(s)).collect(Collectors.toList());
    }
}
