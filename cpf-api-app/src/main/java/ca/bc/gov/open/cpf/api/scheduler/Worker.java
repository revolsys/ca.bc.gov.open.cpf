package ca.bc.gov.open.cpf.api.scheduler;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.revolsys.util.Maps;
import com.revolsys.util.Property;

public class Worker {
  private final String id;

  private Timestamp lastConnectTime;

  private final Map<String, BatchJobRequestExecutionGroup> executingGroupsById = new TreeMap<>();

  private final Map<String, Set<BatchJobRequestExecutionGroup>> executingGroupsIdByModule = new TreeMap<>();

  private int maxMessageId = 1;

  private final Map<Integer, Map<String, Object>> messages = new TreeMap<Integer, Map<String, Object>>();

  private final Map<String, WorkerModuleState> moduleStates = new TreeMap<String, WorkerModuleState>();

  private final long startTime;

  public Worker(final String id, final long startTime) {
    this.id = id;
    this.startTime = startTime;
  }

  public void addExecutingGroup(final String moduleName,
    final long moduleStartTime, final BatchJobRequestExecutionGroup group) {
    synchronized (this.executingGroupsById) {
      final String groupId = group.getBaseId();
      this.executingGroupsById.put(groupId, group);
      group.setModuleStartTime(moduleStartTime);
      final String moduleNameAndTime = moduleName + ":" + moduleStartTime;
      Maps.addToSet(this.executingGroupsIdByModule,
        moduleNameAndTime, group);
    }
  }

  public void addMessage(final Map<String, Object> message) {
    synchronized (this.messages) {
      final int messageId = this.maxMessageId++;
      this.messages.put(messageId, message);
    }
  }

  public boolean cancelBatchJob(final long batchJobId) {
    synchronized (this.executingGroupsById) {
      boolean found = false;
      for (final Iterator<Entry<String, BatchJobRequestExecutionGroup>> iterator = this.executingGroupsById.entrySet()
        .iterator(); iterator.hasNext();) {
        final Entry<String, BatchJobRequestExecutionGroup> entry = iterator.next();
        final String groupId = entry.getKey();
        final BatchJobRequestExecutionGroup group = entry.getValue();
        if (group.getBatchJobId() == batchJobId) {
          found = true;
          final Map<String, Object> message = new LinkedHashMap<>();
          message.put("action", "cancelGroup");
          message.put("batchJobId", batchJobId);
          message.put("groupId", groupId);
          addMessage(message);
        }
      }
      return found;
    }
  }

  public Set<BatchJobRequestExecutionGroup> cancelExecutingGroups(
    final String moduleNameAndTime) {
    synchronized (this.executingGroupsById) {
      final Set<BatchJobRequestExecutionGroup> groups = this.executingGroupsIdByModule.remove(moduleNameAndTime);
      if (groups != null) {
        for (final BatchJobRequestExecutionGroup group : groups) {
          final String groupId = group.getBaseId();
          this.executingGroupsById.remove(groupId);
        }
      }
      return groups;
    }
  }

  public BatchJobRequestExecutionGroup getExecutingGroup(final String groupId) {
    final String[] ids = groupId.split("-");
    final String baseId = ids[0] + "-" + ids[1];
    return this.executingGroupsById.get(baseId);
  }

  public List<BatchJobRequestExecutionGroup> getExecutingGroups() {
    synchronized (this.executingGroupsById) {
      return new ArrayList<BatchJobRequestExecutionGroup>(
        this.executingGroupsById.values());
    }
  }

  public Map<String, BatchJobRequestExecutionGroup> getExecutingGroupsById() {
    return this.executingGroupsById;
  }

  public String getId() {
    return this.id;
  }

  public Timestamp getLastConnectTime() {
    return this.lastConnectTime;
  }

  public Map<String, Map<String, Object>> getMessages(final int maxMessageId) {
    synchronized (this.messages) {
      final Map<String, Map<String, Object>> messages = new LinkedHashMap<>();
      for (final Iterator<Entry<Integer, Map<String, Object>>> iterator = this.messages.entrySet()
        .iterator(); iterator.hasNext();) {
        final Entry<Integer, Map<String, Object>> entry = iterator.next();
        final Integer messageId = entry.getKey();
        if (messageId <= maxMessageId) {
          iterator.remove();
        } else {
          final Map<String, Object> message = entry.getValue();
          messages.put(String.valueOf(messageId), message);
        }
      }
      return messages;
    }
  }

  public List<WorkerModuleState> getModules() {
    return new ArrayList<>(this.moduleStates.values());
  }

  protected WorkerModuleState getModuleState(final String moduleName) {
    if (Property.hasValue(moduleName)) {
      synchronized (this.moduleStates) {
        WorkerModuleState moduleState = this.moduleStates.get(moduleName);
        if (moduleState == null) {
          moduleState = new WorkerModuleState(moduleName);
          this.moduleStates.put(moduleName, moduleState);
        }
        return moduleState;
      }
    } else {
      return null;
    }
  }

  public long getStartTime() {
    return this.startTime;
  }

  public BatchJobRequestExecutionGroup removeExecutingGroup(final String groupId) {
    synchronized (this.executingGroupsById) {
      final String[] ids = groupId.split("-");
      final String baseId = ids[0] + "-" + ids[1];
      final BatchJobRequestExecutionGroup group = this.executingGroupsById.remove(baseId);
      if (group != null) {
        final String moduleName = group.getModuleName();
        final long moduleStartTime = group.getModuleStartTime();
        final String moduleNameAndTime = moduleName + ":" + moduleStartTime;

        Maps.removeFromCollection(this.executingGroupsIdByModule,
          moduleNameAndTime, group);
      }
      return group;
    }
  }

  public void setLastConnectTime(final Timestamp lastConnectTime) {
    this.lastConnectTime = lastConnectTime;
  }

  @Override
  public String toString() {
    return getId();
  }
}
