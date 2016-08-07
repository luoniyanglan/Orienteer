package org.orienteer.bpm.camunda.handler;

import com.orientechnologies.orient.core.metadata.schema.OType;
import org.camunda.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.camunda.bpm.engine.impl.db.ListQueryParameterObject;
import org.camunda.bpm.engine.impl.history.event.HistoricDecisionInstanceEntity;
import org.orienteer.bpm.camunda.OPersistenceSession;
import org.orienteer.core.util.OSchemaHelper;

import java.util.List;

/**
 * Created by KMukhov on 07.08.16.
 */
public class HistoricDecisionInstanceEntityHandler extends AbstractEntityHandler<HistoricDecisionInstanceEntity> {

    public static final String OCLASS_NAME = "BPMHistoricDecisionInstance";

    public HistoricDecisionInstanceEntityHandler() {
        super(OCLASS_NAME);
    }

    @Override
    public void applySchema(OSchemaHelper helper) {
        super.applySchema(helper);

        helper.oProperty("decisionDefinitionId", OType.STRING, 10)
                .oProperty("decisionDefinitionKey", OType.STRING, 20)
                .oProperty("decisionDefinitionName", OType.STRING, 30)
                .oProperty("processDefinitionKey", OType.STRING, 40)
                .oProperty("processDefinitionId", OType.STRING, 50)
                .oProperty("processInstanceId", OType.STRING, 60)
                .oProperty("caseDefinitionKey", OType.STRING, 70)
                .oProperty("caseDefinitionId", OType.STRING, 80)
                .oProperty("caseInstanceId", OType.STRING, 90)
                .oProperty("activityInstanceId", OType.STRING, 100)
                .oProperty("activityId", OType.STRING, 110)
                .oProperty("evaluationTime", OType.DATETIME, 120)
                .oProperty("collectResultValue", OType.DOUBLE, 130)
                .oProperty("userId", OType.STRING, 140)
                .oProperty("tenantId", OType.STRING, 150);
    }

    @Statement
    public List<HistoricDecisionInstanceEntity> selectHistoricDecisionInstancesByDecisionDefinitionId(
            OPersistenceSession session, ListQueryParameterObject parameter) {
        return queryList(session, "select from " + getSchemaClass() + " where decisionDefinitionId=?", parameter.getParameter());
    }

    @Statement
    public List<HistoricDecisionInstanceEntity> selectHistoricDecisionInstancesByQueryCriteria(
            OPersistenceSession session, HistoricDecisionInstanceQuery query) {
        return query(session, query);
    }
}