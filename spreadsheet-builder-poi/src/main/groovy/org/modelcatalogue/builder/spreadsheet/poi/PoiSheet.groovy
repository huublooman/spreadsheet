package org.modelcatalogue.builder.spreadsheet.poi

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.modelcatalogue.builder.spreadsheet.api.Row
import org.modelcatalogue.builder.spreadsheet.api.Sheet
import org.modelcatalogue.builder.spreadsheet.api.Workbook

class PoiSheet implements Sheet {

    private final XSSFSheet xssfSheet
    private final PoiWorkbook workbook
    private final String originalName

    private final List<Integer> startPositions = []
    private int nextRowNumber = 0
    private final Set<Integer> autoColumns = new HashSet<Integer>()
    private final Map<Integer, PoiRow> rows = [:]

    PoiSheet(PoiWorkbook workbook, XSSFSheet xssfSheet, String originalName) {
        this.workbook = workbook
        this.xssfSheet = xssfSheet
        this.originalName = originalName
    }


    @Override
    String getName() {
        return originalName
    }

    List<PoiRow> getRows() {
        // TODO: reuse existing rows
        xssfSheet.collect { new PoiRow(this, it as XSSFRow) }
    }

    @Override
    void row() {
        findOrCreateRow nextRowNumber++
    }

    private PoiRow findOrCreateRow(int zeroBasedRowNumber) {
        PoiRow row = rows[zeroBasedRowNumber + 1]

        if (row) {
            return row
        }

        XSSFRow xssfRow = xssfSheet.createRow(zeroBasedRowNumber)

        row = new PoiRow(this, xssfRow)

        rows[zeroBasedRowNumber + 1] = row

        return row
    }

    @Override
    void row(@DelegatesTo(Row.class) @ClosureParams(value=FromString.class, options = "org.modelcatalogue.builder.spreadsheet.api.Row") Closure rowDefinition) {
        PoiRow row = findOrCreateRow nextRowNumber++
        row.with rowDefinition
    }

    @Override
    void row(int oneBasedRowNumber, @DelegatesTo(Row.class) @ClosureParams(value=FromString.class, options = "org.modelcatalogue.builder.spreadsheet.api.Row") Closure rowDefinition) {
        assert oneBasedRowNumber > 0
        nextRowNumber = oneBasedRowNumber

        PoiRow poiRow = findOrCreateRow oneBasedRowNumber - 1
        poiRow.with rowDefinition
    }

    @Override
    PoiWorkbook getWorkbook() {
        return workbook
    }

    @Override
    void freeze(int column, int row) {
        xssfSheet.createFreezePane(column, row)
    }

    @Override
    void freeze(String column, int row) {
        freeze PoiRow.parseColumn(column), row
    }

    @Override
    void collapse(@DelegatesTo(Sheet.class) @ClosureParams(value=FromString.class, options = "org.modelcatalogue.builder.spreadsheet.api.Sheet") Closure insideGroupDefinition) {
        createGroup(true, insideGroupDefinition)
    }

    @Override
    void group(@DelegatesTo(Sheet.class) @ClosureParams(value=FromString.class, options = "org.modelcatalogue.builder.spreadsheet.api.Sheet") Closure insideGroupDefinition) {
        createGroup(false, insideGroupDefinition)
    }

    @Override
    Object getLocked() {
        sheet.enableLocking()
        return null
    }

    @Override
    void password(String password) {
        sheet.protectSheet(password)
    }

    private void createGroup(boolean collapsed, @DelegatesTo(Sheet.class) Closure insideGroupDefinition) {
        startPositions.push nextRowNumber
        with insideGroupDefinition

        int startPosition = startPositions.pop()

        if (nextRowNumber - startPosition > 1) {
            int endPosition = nextRowNumber - 1
            xssfSheet.groupRow(startPosition, endPosition)
            if (collapsed) {
                xssfSheet.setRowGroupCollapsed(endPosition, true)
            }
        }

    }

    protected XSSFSheet getSheet() {
        return xssfSheet
    }

    protected void addAutoColumn(int i) {
        autoColumns << i
    }

    protected void processAutoColumns() {
        for (int index in autoColumns) {
            xssfSheet.autoSizeColumn(index)
        }
    }
}
