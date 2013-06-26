package ca.bc.gov.open.cpf.api.controller;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.ConfigPropertyLoader;
import ca.bc.gov.open.cpf.plugin.impl.module.ClassLoaderModule;
import ca.bc.gov.open.cpf.plugin.impl.module.ClassLoaderModuleLoader;

import com.revolsys.maven.MavenPom;
import com.revolsys.maven.MavenRepository;

public class ConfigPropertyModule extends ClassLoaderModule {

  private final MavenRepository mavenRepository;

  private String mavenModuleId;

  private final Set<String> excludeMavenIds;

  private final ConfigPropertyModuleLoader moduleLoader;

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
    setConfigPropertyLoader(configPropertyLoader);
    setRemoteable(true);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void doStart() {
    if (isEnabled() && !isStarted()) {
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
        if (!hasError()) {
          super.doStart();
        }
      } catch (final Throwable e) {
        addModuleError(e);
      }
      if (hasError()) {
        stop();
      } else {
        final BatchJobService batchJobService = moduleLoader.getBatchJobService();
        batchJobService.collateStatistics();
      }
    }
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void doStop() {
    if (isStarted()) {
      final List<String> businessApplicationNames = getBusinessApplicationNames();
      setStartedDate(null);

      super.doStop();
      setClassLoader(null);
      setConfigUrl(null);
      final BatchJobService batchJobService = moduleLoader.getBatchJobService();
      batchJobService.scheduleSaveStatistics(businessApplicationNames);
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
    moduleLoader.refreshConfigProperties(this);
    moduleLoader.refreshUserGroup(this, "_ADMIN",
      "Application administrator for ");
    moduleLoader.refreshUserGroup(this, "_SECURITY",
      "Security administrator for ");
    moduleLoader.refreshUserGroups(this);
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
