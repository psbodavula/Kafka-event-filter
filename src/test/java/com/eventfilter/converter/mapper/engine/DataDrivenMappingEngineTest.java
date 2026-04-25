package com.eventfilter.converter.mapper.engine;

import com.eventfilter.converter.mapper.config.ConditionEntry;
import com.eventfilter.converter.mapper.config.MappingConfiguration;
import com.eventfilter.converter.mapper.config.MappingRuleDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataDrivenMappingEngineTest {

    private DataDrivenMappingEngine engine;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        engine = new DataDrivenMappingEngine(objectMapper);
        engine.init();
    }

    @Test
    void shouldResolveNestedXpath() {
        Map<String, Object> source = Map.of(
                "pmtInf", Map.of(
                        "pmtId", Map.of("uetr", "97ed4827-7b6f-4491-a06f-b548d5a7512d")
                )
        );

        Object result = engine.resolveXpath(source, "pmtInf/pmtId/uetr");
        assertEquals("97ed4827-7b6f-4491-a06f-b548d5a7512d", result);
    }

    @Test
    void shouldReturnNullForMissingXpath() {
        Map<String, Object> source = Map.of("pmtInf", Map.of("pmtId", Map.of()));
        assertNull(engine.resolveXpath(source, "pmtInf/pmtId/uetr"));
    }

    @Test
    void shouldMapAsIsRule() {
        MappingRuleDefinition rule = MappingRuleDefinition.builder()
                .targetField("uetr")
                .type("MAP_AS_IS")
                .sourceXpath("pmtInf/pmtId/uetr")
                .presence("REQUIRED")
                .build();

        MappingConfiguration config = MappingConfiguration.builder()
                .mappingName("TEST")
                .rules(List.of(rule))
                .build();

        Map<String, Object> source = Map.of(
                "pmtInf", Map.of("pmtId", Map.of("uetr", "test-uetr-123"))
        );

        MappingResult result = engine.map(config, source);
        assertFalse(result.hasErrors());
        assertEquals("test-uetr-123", result.getOutputFields().get("uetr"));
    }

    @Test
    void shouldReportErrorForMissingRequiredField() {
        MappingRuleDefinition rule = MappingRuleDefinition.builder()
                .targetField("uetr")
                .type("MAP_AS_IS")
                .sourceXpath("pmtInf/pmtId/uetr")
                .presence("REQUIRED")
                .errorOnMissing("UETR is required")
                .build();

        MappingConfiguration config = MappingConfiguration.builder()
                .mappingName("TEST")
                .rules(List.of(rule))
                .build();

        Map<String, Object> source = Map.of("pmtInf", Map.of("pmtId", Map.of()));

        MappingResult result = engine.map(config, source);
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().get(0).contains("UETR is required"));
    }

    @Test
    void shouldMapConditionalRule_WellsFargo() {
        MappingRuleDefinition rule = MappingRuleDefinition.builder()
                .targetField("from")
                .type("CONDITIONAL")
                .sourceXpath("pmtInf/instdAgt/finInstnId/clrSysMmbId/mmbId")
                .presence("REQUIRED")
                .conditions(List.of(
                        ConditionEntry.builder()
                                .matchValues(List.of("121000248", "0407"))
                                .outputValue("WFBIUS6SXXX")
                                .build(),
                        ConditionEntry.builder()
                                .matchValues(List.of("026005092", "0509"))
                                .outputValue("PNBPUS3NNYC")
                                .build()
                ))
                .errorOnNoMatch("Unknown mmbId")
                .build();

        MappingConfiguration config = MappingConfiguration.builder()
                .mappingName("TEST")
                .rules(List.of(rule))
                .build();

        Map<String, Object> source = Map.of(
                "pmtInf", Map.of(
                        "instdAgt", Map.of(
                                "finInstnId", Map.of(
                                        "clrSysMmbId", Map.of("mmbId", "121000248")
                                )
                        )
                )
        );

        MappingResult result = engine.map(config, source);
        assertFalse(result.hasErrors());
        assertEquals("WFBIUS6SXXX", result.getOutputFields().get("from"));
    }

    @Test
    void shouldMapConditionalRule_PNC() {
        MappingRuleDefinition rule = MappingRuleDefinition.builder()
                .targetField("from")
                .type("CONDITIONAL")
                .sourceXpath("pmtInf/instdAgt/finInstnId/clrSysMmbId/mmbId")
                .conditions(List.of(
                        ConditionEntry.builder()
                                .matchValues(List.of("121000248", "0407"))
                                .outputValue("WFBIUS6SXXX")
                                .build(),
                        ConditionEntry.builder()
                                .matchValues(List.of("026005092", "0509"))
                                .outputValue("PNBPUS3NNYC")
                                .build()
                ))
                .build();

        MappingConfiguration config = MappingConfiguration.builder()
                .mappingName("TEST")
                .rules(List.of(rule))
                .build();

        Map<String, Object> source = Map.of(
                "pmtInf", Map.of(
                        "instdAgt", Map.of(
                                "finInstnId", Map.of(
                                        "clrSysMmbId", Map.of("mmbId", "0509")
                                )
                        )
                )
        );

        MappingResult result = engine.map(config, source);
        assertFalse(result.hasErrors());
        assertEquals("PNBPUS3NNYC", result.getOutputFields().get("from"));
    }

    @Test
    void shouldMapTransactionStatus_Completed() {
        MappingRuleDefinition rule = MappingRuleDefinition.builder()
                .targetField("transaction_status")
                .type("CONDITIONAL")
                .sourceXpath("wfEvtInf/evtNm")
                .presence("REQUIRED")
                .conditions(List.of(
                        ConditionEntry.builder()
                                .matchValues(List.of("PAYMENT_EXECUTION.EXECUTION.COMPLETED"))
                                .outputValue("ACCC")
                                .build(),
                        ConditionEntry.builder()
                                .matchValues(List.of("PAYMENT_EXECUTION.RETURN.COMPLETED"))
                                .outputValue("RJCT")
                                .build(),
                        ConditionEntry.builder()
                                .matchValues(List.of("PAYMENT_EXECUTION.EXECUTION.RING_FENCED"))
                                .outputValue("ACSP")
                                .build()
                ))
                .errorOnNoMatch("not GPI eligible")
                .build();

        MappingConfiguration config = MappingConfiguration.builder()
                .mappingName("TEST")
                .rules(List.of(rule))
                .build();

        Map<String, Object> source = Map.of(
                "wfEvtInf", Map.of("evtNm", "PAYMENT_EXECUTION.EXECUTION.COMPLETED")
        );

        MappingResult result = engine.map(config, source);
        assertFalse(result.hasErrors());
        assertEquals("ACCC", result.getOutputFields().get("transaction_status"));
    }

    @Test
    void shouldMapTransactionStatus_Returned() {
        MappingRuleDefinition rule = MappingRuleDefinition.builder()
                .targetField("transaction_status")
                .type("CONDITIONAL")
                .sourceXpath("wfEvtInf/evtNm")
                .conditions(List.of(
                        ConditionEntry.builder()
                                .matchValues(List.of("PAYMENT_EXECUTION.EXECUTION.COMPLETED"))
                                .outputValue("ACCC")
                                .build(),
                        ConditionEntry.builder()
                                .matchValues(List.of("PAYMENT_EXECUTION.RETURN.COMPLETED"))
                                .outputValue("RJCT")
                                .build(),
                        ConditionEntry.builder()
                                .matchValues(List.of("PAYMENT_EXECUTION.EXECUTION.RING_FENCED"))
                                .outputValue("ACSP")
                                .build()
                ))
                .build();

        MappingConfiguration config = MappingConfiguration.builder()
                .mappingName("TEST")
                .rules(List.of(rule))
                .build();

        Map<String, Object> source = Map.of(
                "wfEvtInf", Map.of("evtNm", "PAYMENT_EXECUTION.RETURN.COMPLETED")
        );

        MappingResult result = engine.map(config, source);
        assertEquals("RJCT", result.getOutputFields().get("transaction_status"));
    }

    @Test
    void shouldMapTransactionStatus_RingFenced() {
        MappingRuleDefinition rule = MappingRuleDefinition.builder()
                .targetField("transaction_status")
                .type("CONDITIONAL")
                .sourceXpath("wfEvtInf/evtNm")
                .conditions(List.of(
                        ConditionEntry.builder()
                                .matchValues(List.of("PAYMENT_EXECUTION.EXECUTION.RING_FENCED"))
                                .outputValue("ACSP")
                                .build()
                ))
                .build();

        MappingConfiguration config = MappingConfiguration.builder()
                .mappingName("TEST")
                .rules(List.of(rule))
                .build();

        Map<String, Object> source = Map.of(
                "wfEvtInf", Map.of("evtNm", "PAYMENT_EXECUTION.EXECUTION.RING_FENCED")
        );

        MappingResult result = engine.map(config, source);
        assertEquals("ACSP", result.getOutputFields().get("transaction_status"));
    }

    @Test
    void shouldReportErrorForUnknownEventStatus() {
        MappingRuleDefinition rule = MappingRuleDefinition.builder()
                .targetField("transaction_status")
                .type("CONDITIONAL")
                .sourceXpath("wfEvtInf/evtNm")
                .conditions(List.of(
                        ConditionEntry.builder()
                                .matchValues(List.of("PAYMENT_EXECUTION.EXECUTION.COMPLETED"))
                                .outputValue("ACCC")
                                .build()
                ))
                .errorOnNoMatch("not GPI eligible for FED/CHP inbound")
                .build();

        MappingConfiguration config = MappingConfiguration.builder()
                .mappingName("TEST")
                .rules(List.of(rule))
                .build();

        Map<String, Object> source = Map.of(
                "wfEvtInf", Map.of("evtNm", "SOME.UNKNOWN.EVENT")
        );

        MappingResult result = engine.map(config, source);
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().get(0).contains("not GPI eligible"));
    }

    @Test
    void shouldApplyHardcodeRule() {
        MappingRuleDefinition rule = MappingRuleDefinition.builder()
                .targetField("tracker_informing_party")
                .type("HARDCODE")
                .hardcodedValue("TESTBICXXX")
                .build();

        MappingConfiguration config = MappingConfiguration.builder()
                .mappingName("TEST")
                .rules(List.of(rule))
                .build();

        MappingResult result = engine.map(config, Map.of());
        assertEquals("TESTBICXXX", result.getOutputFields().get("tracker_informing_party"));
    }

    @Test
    void shouldApplyExpressionRule() {
        MappingRuleDefinition rule = MappingRuleDefinition.builder()
                .targetField("last_update_time")
                .type("EXPRESSION")
                .expression("CURRENT_TIMESTAMP")
                .build();

        MappingConfiguration config = MappingConfiguration.builder()
                .mappingName("TEST")
                .rules(List.of(rule))
                .build();

        MappingResult result = engine.map(config, Map.of());
        assertNotNull(result.getOutputFields().get("last_update_time"));
    }

    @Test
    void shouldApplyPresenceCheckWithThenRules() {
        MappingRuleDefinition thenRule = MappingRuleDefinition.builder()
                .targetField("from")
                .type("CONDITIONAL")
                .sourceXpath("pmtInf/instdAgt/finInstnId/clrSysMmbId/mmbId")
                .conditions(List.of(
                        ConditionEntry.builder()
                                .matchValues(List.of("121000248"))
                                .outputValue("WFBIUS6SXXX")
                                .build()
                ))
                .build();

        MappingRuleDefinition rule = MappingRuleDefinition.builder()
                .targetField("from")
                .type("PRESENCE_CHECK")
                .sourceXpath("pmtInf/instdAgt/finInstnId/clrSysMmbId/mmbId")
                .presence("REQUIRED")
                .thenRules(List.of(thenRule))
                .fallbackSourceXpath("pmtInf/instdAgt/finInstnId/bicFi")
                .errorOnMissing("mmbId missing")
                .build();

        MappingConfiguration config = MappingConfiguration.builder()
                .mappingName("TEST")
                .rules(List.of(rule))
                .build();

        Map<String, Object> source = Map.of(
                "pmtInf", Map.of(
                        "instdAgt", Map.of(
                                "finInstnId", Map.of(
                                        "clrSysMmbId", Map.of("mmbId", "121000248")
                                )
                        )
                )
        );

        MappingResult result = engine.map(config, source);
        assertFalse(result.hasErrors());
        assertEquals("WFBIUS6SXXX", result.getOutputFields().get("from"));
    }

    @Test
    void shouldUseFallbackWhenPresenceCheckFails() {
        MappingRuleDefinition rule = MappingRuleDefinition.builder()
                .targetField("from")
                .type("PRESENCE_CHECK")
                .sourceXpath("pmtInf/instdAgt/finInstnId/clrSysMmbId/mmbId")
                .presence("OPTIONAL")
                .fallbackSourceXpath("pmtInf/instdAgt/finInstnId/bicFi")
                .build();

        MappingConfiguration config = MappingConfiguration.builder()
                .mappingName("TEST")
                .rules(List.of(rule))
                .build();

        Map<String, Object> source = Map.of(
                "pmtInf", Map.of(
                        "instdAgt", Map.of(
                                "finInstnId", Map.of("bicFi", "WFBIUS6SXXX")
                        )
                )
        );

        MappingResult result = engine.map(config, source);
        assertFalse(result.hasErrors());
        assertEquals("WFBIUS6SXXX", result.getOutputFields().get("from"));
    }

    @Test
    void shouldSkipOptionalMissingFields() {
        MappingRuleDefinition rule = MappingRuleDefinition.builder()
                .targetField("transaction_status_date")
                .type("MAP_AS_IS")
                .sourceXpath("wfEvtInf/evtDtTm")
                .presence("OPTIONAL")
                .build();

        MappingConfiguration config = MappingConfiguration.builder()
                .mappingName("TEST")
                .rules(List.of(rule))
                .build();

        MappingResult result = engine.map(config, Map.of());
        assertFalse(result.hasErrors());
        assertNull(result.getOutputFields().get("transaction_status_date"));
    }

    @Test
    void shouldLoadAndApplyFullConfiguration() {
        Map<String, Object> source = new HashMap<>();

        Map<String, Object> pmtId = new HashMap<>();
        pmtId.put("uetr", "97ed4827-7b6f-4491-a06f-b548d5a7512d");

        Map<String, Object> mmbIdMap = new HashMap<>();
        mmbIdMap.put("mmbId", "121000248");

        Map<String, Object> finInstnId = new HashMap<>();
        finInstnId.put("clrSysMmbId", mmbIdMap);

        Map<String, Object> instdAgt = new HashMap<>();
        instdAgt.put("finInstnId", finInstnId);

        Map<String, Object> pmtInf = new HashMap<>();
        pmtInf.put("pmtId", pmtId);
        pmtInf.put("instdAgt", instdAgt);

        Map<String, Object> wfEvtInf = new HashMap<>();
        wfEvtInf.put("evtNm", "PAYMENT_EXECUTION.EXECUTION.COMPLETED");
        wfEvtInf.put("evtDtTm", "2025-04-25T10:30:00Z");

        source.put("pmtInf", pmtInf);
        source.put("wfEvtInf", wfEvtInf);

        MappingResult result = engine.map("UPO_TO_GPI_STATUS_UPDATE", source);

        assertFalse(result.hasErrors(), "Errors: " + result.getErrors());
        assertEquals("97ed4827-7b6f-4491-a06f-b548d5a7512d", result.getOutputFields().get("uetr"));
        assertEquals("WFBIUS6SXXX", result.getOutputFields().get("from"));
        assertEquals("ACCC", result.getOutputFields().get("transaction_status"));
        assertEquals("2025-04-25T10:30:00Z", result.getOutputFields().get("transaction_status_date"));
        assertNotNull(result.getOutputFields().get("last_update_time"));
    }

    @Test
    void shouldHandleMultipleRulesWithMixedResults() {
        Map<String, Object> source = new HashMap<>();

        Map<String, Object> pmtId = new HashMap<>();
        pmtId.put("uetr", "test-uetr");

        Map<String, Object> pmtInf = new HashMap<>();
        pmtInf.put("pmtId", pmtId);
        source.put("pmtInf", pmtInf);

        Map<String, Object> wfEvtInf = new HashMap<>();
        wfEvtInf.put("evtNm", "SOME.UNKNOWN.EVENT");
        source.put("wfEvtInf", wfEvtInf);

        MappingResult result = engine.map("UPO_TO_GPI_STATUS_UPDATE", source);

        assertEquals("test-uetr", result.getOutputFields().get("uetr"));
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("not GPI eligible")));
    }
}
