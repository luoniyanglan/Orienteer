package org.orienteer.bpm.camunda.handler;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.wicket.util.string.Strings;
import org.camunda.bpm.engine.impl.db.ListQueryParameterObject;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.JobEntity;
import org.camunda.bpm.engine.impl.persistence.entity.MessageEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TimerEntity;
import org.camunda.bpm.engine.runtime.JobQuery;
import org.orienteer.bpm.camunda.OPersistenceSession;
import org.orienteer.core.util.OSchemaHelper;

import com.github.raymanrt.orientqb.query.Clause;
import com.github.raymanrt.orientqb.query.Operator;
import com.github.raymanrt.orientqb.query.Parameter;
import com.github.raymanrt.orientqb.query.Query;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * {@link IEntityHandler} for {@link JobEntity}
 */
public class JobEntityHandler extends AbstractEntityHandler<JobEntity> {

	public static final String OCLASS_NAME = "BPMJob";
	
	public JobEntityHandler() {
		super(OCLASS_NAME);
	}
	
	@Override
	public void applySchema(OSchemaHelper helper) {
		super.applySchema(helper);
		helper.oProperty("type", OType.STRING, 10)
			  .oProperty("duedate", OType.DATETIME, 20)
			  .oProperty("lockExpirationTime", OType.DATETIME, 30)
			  .oProperty("lockOwner", OType.STRING, 40)
			  .oProperty("exclusive", OType.BOOLEAN, 50)
			  .oProperty("executionId", OType.STRING, 60)
			  .oProperty("processInstanceId", OType.STRING, 70)
			  .oProperty("executionId", OType.STRING, 80)
			  .oProperty("processDefinitionId", OType.STRING, 90)
			  .oProperty("processDefinitionKey", OType.STRING, 100)
			  .oProperty("retries", OType.INTEGER, 110)
			  .oProperty("exceptionByteArrayId", OType.STRING, 120)
			  .oProperty("exceptionMessage", OType.STRING, 130)
			  .oProperty("repeat", OType.STRING, 140)
			  .oProperty("jobHandlerType", OType.STRING, 150)
			  .oProperty("JobHandlerConfigurationRaw", OType.STRING, 160)
			  .oProperty("deploymentId", OType.STRING, 170)
			  .oProperty("suspensionState", OType.INTEGER, 180)
			  .oProperty("jobDefinitionId", OType.STRING, 190)
			  .oProperty("sequenceCounter", OType.LONG, 200)
			  .oProperty("priority", OType.LONG, 210);
	}
	
	@Override
	public JobEntity mapToEntity(ODocument doc, JobEntity entity, OPersistenceSession session) {
		if(entity==null) {
			String type = doc.field("type");
			if(TimerEntity.TYPE.equals(type)) entity = new TimerEntity();
			else if(MessageEntity.TYPE.equals(type)) entity = new MessageEntity();
		}
		entity =  super.mapToEntity(doc, entity, session);
		String exceptionByteArrayId = doc.field("exceptionByteArrayId");
		//JobEntity doesn't allow to set exceptionByteArrayId: so lets inject it.
		//TODO: Fix this behavior
		if(exceptionByteArrayId!=null) {
			try {
				Field field = entity.getClass().getDeclaredField("exceptionByteArrayId");
				field.setAccessible(true);
				field.set(entity, exceptionByteArrayId);
			} catch (Exception e) {
				logger.warn("Setting exceptionByteArrayId doesn't work", e);
			}
		}
		return entity;
	}
	
	@Statement
	public List<JobEntity> selectJobByQueryCriteria(OPersistenceSession session, JobQuery query) {
		return  query(session, query);
	}
	
	@Statement
	public List<JobEntity> selectJobsByConfiguration(OPersistenceSession session, ListQueryParameterObject query) {
		Map<String, Object> params = (Map<String, Object>) query.getParameter(); 
	    String config = (String) params.get("handlerConfiguration");
	    String followUpConfig = (String) params.get("handlerConfigurationWithFollowUpJobCreatedProperty");
	    String type = (String) params.get("handlerType");
	    List<String> args = new ArrayList<>();
	    Query q = new Query().from(getSchemaClass());
	    q.where(Clause.clause("jobHandlerType", Operator.EQ, Parameter.PARAMETER));
	    args.add(type);
	    Clause eqConfig = Clause.clause("JobHandlerConfigurationRaw", Operator.EQ, Parameter.PARAMETER);
	    if(Strings.isEmpty(followUpConfig)) {
	    	q.where(eqConfig);
	    	args.add(config);
	    } else {
	    	q.where(Clause.or(eqConfig, eqConfig));
	    	args.add(config); 
	    	args.add(followUpConfig);
	    }
	    return queryList(session, q.toString(), args.toArray());
	}
	
	@Statement
	public List<JobEntity> selectExclusiveJobsToExecute(OPersistenceSession session, ListQueryParameterObject query) {
		Map<String, Object> params = (Map<String, Object>) query.getParameter();
	    Date now=(Date) params.get("now");
	    int maxResults=query.getMaxResults();
	    return queryList(session, "select from "+getSchemaClass()+
	    					" where retries > 0"
	    					+ " and (duedate is null or duedate <= ?)"
	    					+ " and (lockOwner is null or lockExpirationTime < ?)"
	    					+ " and exclusive = true"
	    					+ " and suspensionState = 1"
	    					+ " LIMIT ?", now, now, maxResults);
	}
	
	@Statement
	public List<JobEntity> selectNextJobsToExecute(OPersistenceSession session, ListQueryParameterObject query) {
		Map<String, Object> params = (Map<String, Object>) query.getParameter();
	    Date now=(Date) params.get("now");
	    int maxResults=query.getMaxResults();
	    String orderBy = query.getOrderBy();
	    return queryList(session, "select from "+getSchemaClass()+
	    					" where retries > 0"
	    					+ " and (duedate is null or duedate <= ?)"
	    					+ " and (lockOwner is null or lockExpirationTime < ?)"
	    					+ " and suspensionState = 1"
	    					+ (Strings.isEmpty(orderBy)?" order by "+orderBy:"")
	    					+ " LIMIT ?", now, now, maxResults);
	}
	
	@Statement
	public JobEntity selectJob(OPersistenceSession session, String id) {
		return read(id, session);
	}
	
	@Statement
	public List<JobEntity> selectJobsByExecutionId(OPersistenceSession session, ListQueryParameterObject query) {
		return queryList(session, "select from "+getSchemaClass()+" where executionId = ?", query.getParameter());
	}
	
}