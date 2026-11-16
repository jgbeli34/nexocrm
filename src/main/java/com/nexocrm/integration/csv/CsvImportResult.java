package com.nexocrm.integration.csv;

import java.util.List;

public record CsvImportResult(int totalRows, int imported, int skipped, List<RowError> errors) {

    public record RowError(long row, String message) {
    }
}
