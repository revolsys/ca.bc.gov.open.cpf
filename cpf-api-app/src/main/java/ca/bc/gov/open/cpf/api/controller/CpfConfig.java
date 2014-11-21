package ca.bc.gov.open.cpf.api.controller;

import java.beans.PropertyChangeSupport;

import com.revolsys.beans.PropertyChangeSupportProxy;

public class CpfConfig implements PropertyChangeSupportProxy {

  private int preProcessPoolSize = 10;

  private int postProcessPoolSize = 10;

  private int schedulerPoolSize = 10;

  private int groupResultPoolSize = 10;

  private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(
    this);

  public int getGroupResultPoolSize() {
    return this.groupResultPoolSize;
  }

  public int getPostProcessPoolSize() {
    return this.postProcessPoolSize;
  }

  public int getPreProcessPoolSize() {
    return this.preProcessPoolSize;
  }

  @Override
  public PropertyChangeSupport getPropertyChangeSupport() {
    return this.propertyChangeSupport;
  }

  public int getSchedulerPoolSize() {
    return this.schedulerPoolSize;
  }

  public void setGroupResultPoolSize(final int groupResultPoolSize) {
    if (groupResultPoolSize < 1) {
      throw new IllegalArgumentException("groupResultPoolSize must be > 1 not "
          + groupResultPoolSize);
    }
    final int oldValue = this.groupResultPoolSize;
    this.groupResultPoolSize = groupResultPoolSize;
    this.propertyChangeSupport.firePropertyChange("groupResultPoolSize",
      oldValue, groupResultPoolSize);
  }

  public void setPostProcessPoolSize(final int postProcessPoolSize) {
    if (postProcessPoolSize < 1) {
      throw new IllegalArgumentException("postProcessPoolSize must be > 1 not "
          + postProcessPoolSize);
    }
    final int oldValue = this.postProcessPoolSize;
    this.postProcessPoolSize = postProcessPoolSize;
    this.propertyChangeSupport.firePropertyChange("postProcessPoolSize",
      oldValue, postProcessPoolSize);
  }

  public void setPreProcessPoolSize(final int preProcessPoolSize) {
    if (preProcessPoolSize < 1) {
      throw new IllegalArgumentException("preProcessPoolSize must be > 1 not "
          + preProcessPoolSize);
    }
    final int oldValue = this.preProcessPoolSize;
    this.preProcessPoolSize = preProcessPoolSize;
    this.propertyChangeSupport.firePropertyChange("preProcessPoolSize",
      oldValue, preProcessPoolSize);
  }

  public void setSchedulerPoolSize(final int schedulerPoolSize) {
    if (schedulerPoolSize < 1) {
      throw new IllegalArgumentException("schedulerPoolSize must be > 1 not "
          + schedulerPoolSize);
    }
    final int oldValue = this.schedulerPoolSize;
    this.schedulerPoolSize = schedulerPoolSize;
    this.propertyChangeSupport.firePropertyChange("schedulerPoolSize",
      oldValue, schedulerPoolSize);
  }
}
