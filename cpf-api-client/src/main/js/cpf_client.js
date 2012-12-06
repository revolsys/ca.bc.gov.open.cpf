/**
 * <p>Construct a new Cpf client that is connected to a specific CPF
 * web service (e.g. http://apps.gov.bc.ca/pub/cpf/ws).</p>
 * 
 * @param {string} url The url to the CPF web service.
 * @constructor
 */
function CpfClient(url) {
  if (url.lastIndexOf('/') == url.length - 1) {
    this.url = url.substring(0, url.length - 1);
  } else {
    this.url = url;
  }
};

/**
 * 
 */
CpfClient.prototype.getUserId = function(callback, errorCallback) {
  this.getJsonIfLoggedIn('/users', {}, function(result) {
    var userId = null;
    var resources = result['resources'];
    if (resources) {
      var resource = resources[0];
      if (resource) {
        userId = resource['userId'];
      }
    }
    callback(userId);
  }, errorCallback);
};

/**
 * 
 */
CpfClient.prototype.getBusinessApplicationNames = function(
  callback,
  errorCallback) {
  this.getResourcesValues(
    '/apps/',
    'businessApplicationName',
    callback,
    errorCallback);
};

/**
 * 
 */
CpfClient.prototype.getBusinessApplicationSpecification = function(
  businessApplicationName,
  businessApplicationVersion,
  callback,
  errorCallback) {
  var path = '/apps/' + businessApplicationName + '/'
      + businessApplicationVersion + '/specification/';
  this.getJsonIfLoggedIn(path, {}, callback, errorCallback);
};

/**
 * 
 */
CpfClient.prototype.getBusinessApplicationVersions = function(
  businessApplicationName,
  callback,
  errorCallback) {
  this.getResourcesValues(
    '/apps/' + businessApplicationName + '/',
    'businessApplicationVersion',
    callback,
    errorCallback);
};

/**
 * 
 */
CpfClient.prototype.getJobErrorResults = function(
  batchJobIdUrl,
  callback,
  errorCallback) {
  this.getJobResultFileList(
    batchJobIdUrl,
    function(resultFiles) {
      var len = resultFiles.length;
      for (var i = 0; i < len; i++) {
        var resultFile = resultFiles[i];
        if (resultFile['batchJobResultType'] == 'errorResultData') {
          callback(resultFile['resourceUri']);
          return;
        }
      }
      callback(null);
    },
    errorCallback);
};

/**
 * 
 */
CpfClient.prototype.getJobIdUrls = function(userId, callback, errorCallback) {
  var self = this;
  if (!userId) {
    this.getUserId(function(userId) {
      if (userId) {
        self.getJobIdUrls(userId, callback, errorCallback);
      } else {
        callback(new Array());
      }
    });
  } else {
    this.getResourcesValues(
      '/users/' + userId + '/jobs/',
      'resourceUri',
      callback,
      errorCallback);
  }
};

/**
 * 
 */
CpfClient.prototype.getJobResultFileList = function(
  batchJobIdUrl,
  callback,
  errorCallback) {
  var path = batchJobIdUrl.substring(this.url.length) + '/results/';
  this.getJsonIfLoggedIn(
    path,
    {},
    function (results) {
      var resources = results['resources'];
      if (resources) {
        callback(resources);
      } else {
        callback(new Array());
      }
    },
    errorCallback
  );
};


/**
 * 
 */
CpfClient.prototype.getJobStructuredResults = function(
  batchJobIdUrl,
  callback,
  errorCallback) {
  var self = this;
  this.getJobResultFileList(
    batchJobIdUrl,
    function(resultFiles) {
      var len = resultFiles.length;
      for (var i = 0; i < len; i++) {
        var resultFile = resultFiles[i];
        if (resultFile['batchJobResultType'] == 'structuredResultData') {
          var resultUri = resultFile['resourceUri'];
          var resultContentType = resultFile['batchJobResultContentType'];
          
          var path = resultUri.substring(self.url.length);
          if (resultContentType == 'application/json') {
            self.getJsonIfLoggedIn(
              path,
              {},
              function (results) {
                if (results) {
                  var items = results['items'];
                  if (items) {
                    callback(items);
                    return;
                  }
                }
                callback('');
              }
            );
          } else {
            self.getTextIfLoggedIn(
              path,
              {},
              function (results) {
                if (results) {
                  callback(results);
                } else {
                  callback('');
                }
              }
            );
          }
          return;
        }
      }
      callback(new Array());
    },
    errorCallback);
};

/**
 * 
 */
CpfClient.prototype.getJobStatus = function(
  batchJobIdUrl,
  callback,
  errorCallback) {
  var path = batchJobIdUrl.substring(this.url.length);
  this.getJsonIfLoggedIn(path, {}, callback, errorCallback);
};

/**
 * 
 */
CpfClient.prototype.submitSingle = function(
  form,
  applicationName,
  applicationVersion) {
  var path = '/apps/' + applicationName + '/' + applicationVersion + '/single/';
  this.submitIfLoggedIn(form, path);
};

/**
 * 
 */
CpfClient.prototype.submitMultiple = function(
  form,
  applicationName,
  applicationVersion) {
  var path = '/apps/' + applicationName + '/' + applicationVersion
      + '/multiple/';
  this.submitIfLoggedIn(form, path);
};

/**
 * @private
 */
CpfClient.prototype.getJson = function(path, data, callback, errorCallback) {
  var url = this.url + path + '?format=json';
    $.ajax({
    url : url,
    dataType : 'jsonp',
    data : data,
    success : function(result) {
      callback(result);
    },
    error : function (xhr, ajaxOptions, thrownError) {
      if (errorCallback) {
        errorCallback(xhr, ajaxOptions, thrownError);
      } else {
        alert(thrownError);
      }
    }
  });
};

/**
 * @private
 */
CpfClient.prototype.getJsonIfLoggedIn = function(path, data, callback) {
  var self = this;
  this.checkLogin(function() {
    self.getJson(path, data, callback);
  }, function() {
    self.openLoginWindow();
    var cpfLoginCheckInterval = 0;
    cpfLoginCheckInterval = setInterval(function() {
      if (!self.isLoginWindowOpen()) {
        clearInterval(cpfLoginCheckInterval);
        self.checkLogin(function() {
          getJson(path, data, callback);
        });
      }
    }, 1000);
  });
};


/**
 * @private
 */
CpfClient.prototype.getText = function(path, data, callback, errorCallback) {
  var url = this.url + path;
    $.ajax({
    url : url,
    dataType : 'text',
    data : data,
    success : function(result) {
      callback(result);
    },
    error : function (xhr, ajaxOptions, thrownError) {
      if (errorCallback) {
        errorCallback(xhr, ajaxOptions, thrownError);
      } else {
        alert(thrownError);
      }
    }
  });
};

/**
 * @private
 */
CpfClient.prototype.getTextIfLoggedIn = function(path, data, callback) {
  var self = this;
  this.checkLogin(function() {
    self.getText(path, data, callback);
  }, function() {
    self.openLoginWindow();
    var cpfLoginCheckInterval = 0;
    cpfLoginCheckInterval = setInterval(function() {
      if (!self.isLoginWindowOpen()) {
        clearInterval(cpfLoginCheckInterval);
        self.checkLogin(function() {
          self.getText(path, data, callback);
        });
      }
    }, 1000);
  });
};

/**
 * @private
 */
CpfClient.prototype.getResourcesValues = function(
  path,
  resourceField,
  callback,
  errorCallback) {
  this.getJsonIfLoggedIn(path, {}, function(result) {
    var resourceValues = new Array();
    var resources = result['resources'];
    if (resources) {
      var len = resources.length;
      for ( var i = 0; i < len; i++) {
        var resource = resources[i];
        var resourceValue = resource[resourceField];
        if (resourceValue) {
          resourceValues.push(resourceValue);
        }
      }
    }
    callback(resourceValues);
  }, errorCallback);
};

/**
 * @private
 */
CpfClient.prototype.submitIfLoggedIn = function(form, path) {
  var self = this;
  form.action = this.url + path;
  this.checkLogin(function() {
    form.submit();
  }, function() {
    self.openLoginWindow();
    var cpfLoginCheckInterval = 0;
    cpfLoginCheckInterval = setInterval(function() {
      if (!self.isLoginWindowOpen()) {
        clearInterval(cpfLoginCheckInterval);
        self.checkLogin(function() {
          form.submit();
        });
      }
    }, 1000);
  });
};

/**
 * @private
 */
CpfClient.prototype.checkLogin = function(loggedInCallback, notLoggedInCallback) {
  if (!notLoggedInCallback) {
    notLoggedInCallback = function(jqXHR, textStatus, errorThrown) {
      alert('Login failed: Submit the form to login again');
    };
  }
  var url = this.url + '/authenticated.json';
  var request = $.ajax({
    url : url,
    dataType : 'jsonp',
    timeout : 10000
  });
  request.success(function(authenticated) {
    if (authenticated['authenticated'] == true) {
      loggedInCallback();
    } else {
      notLoggedInCallback();
    }
  });
  request.error(notLoggedInCallback);
};


/**
 * @private
 */
CpfClient.prototype.openLoginWindow = function() {
  var self = this;
  if (this.isLoginWindowOpen()) {
    this.cpfLoginWindow.focus();
  } else {
    this.cpfLoginWindow = window.open(
      this.url + '/js/closeWindow',
      'cpfLoginWindow');
    var cpfLoginCheckInterval = 0;
    cpfLoginCheckInterval = setInterval(function() {
      if (!self.isLoginWindowOpen()) {
        clearInterval(cpfLoginCheckInterval);
      }
    }, 1000);
  }
};

/**
 * @private
 */
CpfClient.prototype.isLoginWindowOpen = function() {
  if (!this.cpfLoginWindow || this.cpfLoginWindow.closed) {
    return false;
  } else {
    return true;
  }
};
