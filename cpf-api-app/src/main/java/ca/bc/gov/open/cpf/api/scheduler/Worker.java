package ca.bc.gov.open.cpf.api.scheduler;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

public class Worker {
  private final String id;

  private final Set<String> loadedModuleNameTimes = new TreeSet<String>();

  private final Set<String> excludedModules = new TreeSet<String>();

  private Timestamp lastConnectTime;

  private final Map<String, BatchJobRequestExecutionGroup> executingGroupsById = new TreeMap<String, BatchJobRequestExecutionGroup>();

  private final Map<String, List<BatchJobRequestExecutionGroup>> executingGroupsIdByModule = new TreeMap<String, List<BatchJobRequestExecutionGroup>>();

  private final BusinessApplicationRegistry businessApplicationRegistry;

  public Worker(final BusinessApplicationRegistry businessApplicationRegistry,
    final String id) {
    this.businessApplicationRegistry = businessApplicationRegistry;
    this.id = id;
  }

  public void addExcludedModule(final String moduleNameTime) {
    synchronized (excludedModules) {
      excludedModules.add(moduleNameTime);
    }
  }

  public void addExecutingGroup(final String moduleNameAndTime,
    final BatchJobRequestExecutionGroup group) {
    synchronized (executingGroupsById) {
      final String groupId = group.getId();
      executingGroupsById.put(groupId, group);
      List<BatchJobRequestExecutionGroup> groups = executingGroupsIdByModule.get(moduleNameAndTime);
      if (groups == null) {
        groups = new ArrayList<BatchJobRequestExecutionGroup>();
        executingGroupsIdByModule.put(moduleNameAndTime, groups);
      }
      groups.add(group);
    }
  }

  public void addLoadedModule(final String moduleNameTime) {
    synchronized (loadedModuleNameTimes) {
      loadedModuleNameTimes.add(moduleNameTime);
    }
  }

  public List<BatchJobRequestExecutionGroup> cancelExecutingGroups(
    final String moduleNameAndTime) {
    synchronized (executingGroupsById) {
      final List<BatchJobRequestExecutionGroup> groups = executingGroupsIdByModule.remove(moduleNameAndTime);
      if (groups == null) {
        return Collections.emptyList();
      } else {
        for (final BatchJobRequestExecutionGroup group : groups) {
          final String groupId = group.getId();
          executingGroupsById.remove(groupId);
        }
        return groups;
      }
    }
  }

  public Set<String> getExcludedModules() {
    synchronized (excludedModules) {
      return new LinkedHashSet<String>(excludedModules);

    }
  }

  public BatchJobRequestExecutionGroup getExecutingGroup(final String groupId) {
    return executingGroupsById.get(groupId);
  }

  public List<BatchJobRequestExecutionGroup> getExecutingGroups() {
    synchronized (executingGroupsById) {
      return new ArrayList<BatchJobRequestExecutionGroup>(
        executingGroupsById.values());
    }
  }

  public Map<String, BatchJobRequestExecutionGroup> getExecutingGroupsById() {
    return executingGroupsById;
  }

  public String getId() {
    return id;
  }

  public Timestamp getLastConnectTime() {
    return lastConnectTime;
  }

  public Set<String> getLoadedModuleNameTimes() {
    synchronized (loadedModuleNameTimes) {
      return new LinkedHashSet<String>(loadedModuleNameTimes);
    }
  }

  public List<Module> getLoadedModules() {
    final List<Module> modules = new ArrayList<Module>();
    final Collection<String> loadedModuleNames = getLoadedModuleNameTimes();
    for (final String moduleNameTime : loadedModuleNames) {
      final int index = moduleNameTime.lastIndexOf(':');
      final String moduleName = moduleNameTime.substring(0, index);
      final long moduleTime = Long.valueOf(moduleNameTime.substring(index + 1));
      final Module module = businessApplicationRegistry.getModule(moduleName);
      if (module != null) {
        if (moduleTime == module.getStartedTime()) {
          modules.add(module);
        }
      }
    }
    return modules;
  }

  public boolean isModuleLoaded(final String moduleName,
    final long moduleStartTime) {
    synchronized (loadedModuleNameTimes) {
      return loadedModuleNameTimes.contains(moduleName + ":" + moduleStartTime);
    }
  }

  public boolean removeExcludedModule(final String moduleNameTime) {
    synchronized (excludedModules) {
      return excludedModules.remove(moduleNameTime);
    }
  }

  public BatchJobRequestExecutionGroup removeExecutingGroup(final String groupId) {
    synchronized (executingGroupsById) {
      return executingGroupsById.remove(groupId);
    }
  }

  public boolean removeLoadedModule(final String moduleNameTime) {
    synchronized (loadedModuleNameTimes) {
      return loadedModuleNameTimes.remove(moduleNameTime);
    }
  }

  public void setLastConnectTime(final Timestamp lastConnectTime) {
    this.lastConnectTime = lastConnectTime;
  }
}
