function doGet(form) {
  if (form != null) {
    var action = form.action;
    for (element in form.elements) {
      if (element instanceof HTMLInputElement && element.name != '\\') {
        var value = element.value;
        action = action.replace('%7B' + element.name + '%7D', value);
      }
    }
    document.location = action;
  }
}

$(document).ready(function() {
  $('select[name=resultSrid]').change(function() {
    var form = $(this).closest('form');
    var value = $(this).val();
    var scale;
    if (value == 4326 || value == 4269) {
      scale = 10000000;
    } else {
      scale = 1000;
    }
    $('input[name=resultScaleFactorXy]', form).val(scale);
  });
  $('select[name=resultSrid]').each(function() {
    var form = $(this).closest('form');
    var value = $(this).val();
    var scaleField = $('input[name=resultScaleFactorXy]', form);
    var scale = scaleField.val();
    if (value == 4326 || value == 4269) {
      if (scale == 1000) {
        scale = 10000000;
      }
    } else {
      if (scale == 10000000) {
        scale = 1000;
      }
    }
    scaleField.val(scale);    
  });
});

function updateMultiInputDataSubmit() {
  $('#multiInputDataTable table').each(function() {
    var table = $(this).dataTable();
    $('input[name="multiInputDataCount"]').each(function() {
      var count = table.fnGetData().length;
      if (count == 0) {
        $(this).prop('value', '');
      } else {
        $(this).prop('value', count);
      }
      var validator = $('#clientMultiple').validate();
      if (!validator.valid()) {
        validator.form();
      }
    });
  });
}

function addMultiInputDataRow(type) {
  var inputDataContentType = $('#multiInputDataMimeTypeTemplate').html();
  var inputDataField = $('#multiInputData' + type + 'Template').html();
  var inputDataActions = $('#multiInputDataActionsTemplate').html();
  var table = $('#multiInputDataTable table').dataTable();
  table.fnAddData([ inputDataContentType, inputDataField, inputDataActions ]);
  updateMultiInputDataSubmit();
}

function deleteMultiInputDataRow(self) {
  var row = $(self).closest('tr');
  $(row).each(function() {
    var table = $('#multiInputDataTable table').dataTable();
    table.fnDeleteRow(this);
  });
  updateMultiInputDataSubmit();
}