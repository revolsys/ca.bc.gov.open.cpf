function loginUsing(loginUrl) {
  var form = document.getElementById('openIdLoginForm');
  var field = document.getElementById('openid_identifier');
  field.value = loginUrl;
  form.submit();
}