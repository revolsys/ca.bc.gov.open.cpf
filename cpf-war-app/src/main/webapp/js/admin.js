$(document).ready(function() {
  addConfirmButton({
    selector : 'button.stop',
    icon : 'stop',
    title : 'Confirm Stop',
    message : 'Are you sure you want to stop?'
  });
  addConfirmButton({
    selector : 'button.start',
    icon : 'play',
    title : 'Confirm Start',
    message : 'Are you sure you want to start?'
  });
  addConfirmButton({
    selector : 'button.restart',
    icon : 'refresh',
    title : 'Confirm Restart',
    message : 'Are you sure you want to restart?'
  });
  refreshButtons($(document));
});


function generateConsumerSecret() {
  var seed = new Date().getTime();
  var consumerSecret = 'xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      var r = (seed + Math.random()*16)%16 | 0;
      seed = Math.floor(seed/16);
      return (c=='x' ? r : (r&0x7|0x8)).toString(16);
  });
  $('#userAccount input[name=CONSUMER_SECRET]').val(consumerSecret);
}
