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
