package rg.financialplanning.web.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinancialPlanDTO {
    private List<PersonDTO> persons = new ArrayList<>();
    private List<FinancialEntryDTO> entries = new ArrayList<>();
    private Map<String, Double> rates = new HashMap<>();

    public FinancialPlanDTO() {
    }

    public FinancialPlanDTO(List<PersonDTO> persons, List<FinancialEntryDTO> entries, Map<String, Double> rates) {
        this.persons = persons;
        this.entries = entries;
        this.rates = rates;
    }

    public List<PersonDTO> getPersons() {
        return persons;
    }

    public void setPersons(List<PersonDTO> persons) {
        this.persons = persons;
    }

    public List<FinancialEntryDTO> getEntries() {
        return entries;
    }

    public void setEntries(List<FinancialEntryDTO> entries) {
        this.entries = entries;
    }

    public Map<String, Double> getRates() {
        return rates;
    }

    public void setRates(Map<String, Double> rates) {
        this.rates = rates;
    }
}
