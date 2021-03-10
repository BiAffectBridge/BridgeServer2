package org.sagebionetworks.bridge.models.schedules2;

import static org.sagebionetworks.bridge.TestConstants.GUID;
import static org.sagebionetworks.bridge.TestConstants.LABELS;
import static org.testng.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;

import org.testng.annotations.Test;

import org.sagebionetworks.bridge.json.BridgeObjectMapper;

public class AssessmentReferenceTest {
    
    @Test
    public void canSerialize() throws Exception {
        AssessmentReference ref = new AssessmentReference();
        ref.setGuid(GUID);
        ref.setAppId("shared");
        ref.setTitle("Title");
        ref.setMinutesToComplete(10);
        ref.setLabels(LABELS);
        
        JsonNode node = BridgeObjectMapper.get().valueToTree(ref);
        assertEquals(node.get("guid").textValue(), GUID);
        assertEquals(node.get("appId").textValue(), "shared");
        assertEquals(node.get("title").textValue(), "Title");
        assertEquals(node.get("minutesToComplete").intValue(), 10);
        assertEquals(node.get("type").textValue(), "AssessmentReference");
        
        ArrayNode arrayNode = (ArrayNode)node.get("labels");
        assertEquals(arrayNode.get(0).get("lang").textValue(), "en");
        assertEquals(arrayNode.get(0).get("label").textValue(), "English");
        
        assertEquals(arrayNode.get(1).get("lang").textValue(), "fr");
        assertEquals(arrayNode.get(1).get("label").textValue(), "French");
        
        AssessmentReference deser = BridgeObjectMapper.get()
                .readValue(node.toString(), AssessmentReference.class);
        assertEquals(deser.getGuid(), GUID);
        assertEquals(deser.getAppId(), "shared");
        assertEquals(deser.getTitle(), "Title");
        assertEquals(deser.getMinutesToComplete(), Integer.valueOf(10));
        assertEquals(deser.getLabels().get(0).getLabel(), LABELS.get(0).getLabel());
        assertEquals(deser.getLabels().get(1).getLabel(), LABELS.get(1).getLabel());
    }
    
    @Test
    public void nullLabelsReturnsEmptyList() {
        AssessmentReference ref = new AssessmentReference();
        assertEquals(ref.getLabels(), ImmutableList.of());
    }
}
