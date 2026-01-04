package rg.financialplanning.web.dto;

import java.util.List;

public class SensitivityRowDTO {
    private double rate;
    private List<SensitivityCellDTO> cells;

    public SensitivityRowDTO() {
    }

    public SensitivityRowDTO(double rate, List<SensitivityCellDTO> cells) {
        this.rate = rate;
        this.cells = cells;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public List<SensitivityCellDTO> getCells() {
        return cells;
    }

    public void setCells(List<SensitivityCellDTO> cells) {
        this.cells = cells;
    }
}
