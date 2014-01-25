package ca.bc.gov.open.cpf.api.controller;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.ConfigPropertyLoader;
import ca.bc.gov.open.cpf.plugin.impl.module.ClassLoaderModule;
import ca.bc.gov.open.cpf.plugin.impl.module.ClassLoaderModuleLoader;

import com.revolsys.maven.MavenPom;
import com.revolsys.maven.MavenRepository;
import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;

public class ConfigPropertyModule extends ClassLoaderModule {

  private final MavenRepository mavenRepository;

  private String mavenModuleId;

  private final Set<String> excludeMavenIds;

  private final ConfigPropertyModuleLoader moduleLoader;

  private final CpfDataAccessObject dataAccessObject;

  public ConfigPropertyModule(final ConfigPropertyModuleLoader moduleLoader,
    final BusinessApplicationRegistry businessApplicationRegistry,
    final String moduleName, final MavenRepository mavenRepository,
    final String mavenModuleId, final Set<String> excludeMavenIds,
    final ConfigPropertyLoader configPropertyLoader) {
    super(businessApplicationRegistry, moduleName);
    this.moduleLoader = moduleLoader;
    this.mavenRepository = mavenRepository;
    this.mavenModuleId = mavenModuleId;
    this.excludeMavenIds = excludeMavenIds;
    this.dataAccessObject = moduleLoader.getDataAccessObject();
    setConfigPropertyLoader(configPropertyLoader);
    setRemoteable(true);
  }

  @Override
  public void doStart() {

    if (isEnabled()) {
      if (!isStarted()) {
        try (
          Transaction transaction = dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
          try {
            try {
              clearModuleError();

              final ClassLoader classLoader = getClassLoader();
              final List<URL> configUrls = ClassLoaderModuleLoader.getConfigUrls(
                classLoader, false);
              if (configUrls.size() == 1) {
                final URL configUrl = configUrls.get(0);
                setConfigUrl(configUrl);
                setClassLoader(classLoader);
              } else if (configUrls.isEmpty()) {
                addModuleError("No META-INF/ca.bc.gov.open.cpf.plugin.sf.xml resource found for Maven module");
              } else {
                addModuleError("Multiple META-INF/ca.bc.gov.open.cpf.plugin.sf.xml resources found for Maven module");
              }
              if (!isHasError()) {
                super.doStart();
              }
            } catch (final Throwable e) {
              transaction.setRollbackOnly();
              addModuleError(e);
            }
            if (isHasError()) {
              doStop();
            } else {
              final BatchJobService batchJobService = moduleLoader.getBatchJobService();
              batchJobService.collateStatistics();
            }
          } catch (final Throwable e) {
            throw transaction.setRollbackOnly(e);
          }
        }
      }
    } else {
      setStatus("Disabled");
    }
  }

  @Override
  public void doStop() {
    if (isStarted()) {
      try (
        Transaction transaction = dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
        try {
          final List<String> businessApplicationNames = getBusinessApplicationNames();
          setStartedDate(null);

          super.doStop();
          setClassLoader(null);
          setConfigUrl(null);
          final BatchJobService batchJobService = moduleLoader.getBatchJobService();
          batchJobService.scheduleSaveStatistics(businessApplicationNames);
        } catch (final Throwable e) {
          throw transaction.setRollbackOnly(e);
        }
      }
    } else if (isEnabled()) {
      setStatus("Stopped");
    } else {
      setStatus("Disabled");
    }
  }

  @Override
  public ClassLoader getClassLoader() {
    ClassLoader classLoader = super.getClassLoader();
    if (classLoader == null) {
      final Set<String> excludeIds = new HashSet<String>(this.excludeMavenIds);
      for (final String excludeId : this.excludeMavenIds) {
        try {
          excludeIds.add(MavenPom.getGroupAndArtifactId(excludeId));
          final MavenPom pom = mavenRepository.getPom(excludeId);
          for (final String dependencyId : pom.getDependencies(excludeIds)) {
            excludeIds.add(MavenPom.getGroupAndArtifactId(dependencyId));
          }
        } catch (final IllegalArgumentException e) {
        }
      }

      final MavenPom pom = mavenRepository.getPom(mavenModuleId);
      classLoader = pom.createClassLoader(excludeIds);
      setClassLoader(classLoader);
    }
    return classLoader;
  }

  public String getMavenModuleId() {
    return mavenModuleId;
  }

  @Override
  public String getModuleDescriptor() {
    return mavenModuleId;
  }

  @Override
  public String getModuleType() {
    return "Maven";
  }

  @Override
  public boolean isGroupNameValid(final String groupName) {
    return groupName.startsWith(getName() + "_");
  }

  @Override
  protected void preLoadApplications() {
    try (
      Transaction transaction = dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        moduleLoader.refreshConfigProperties(this);
        moduleLoader.refreshUserGroup(this, "_ADMIN",
          "Application administrator for ");
        moduleLoader.refreshUserGroup(this, "_SECURITY",
          "Security administrator for ");
        moduleLoader.refreshUserGroups(this);
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }
  }

  public void setMavenModuleId(final String mavenModuleId) {
    this.mavenModuleId = mavenModuleId;
  }

  public void setModuleDescriptor(final String mavenModuleId) {
    this.mavenModuleId = mavenModuleId;
  }

  @Override
  public String toString() {
    return getName();
  }
}
