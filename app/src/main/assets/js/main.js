$(function () {
	$('#drumpad').on('touchstart', 'a', onClickPad);

	function onClickPad() {
		var sounds = $(this).data('sound').split(',');
		var id = sounds[Math.floor(Math.random() * sounds.length)];
		android.setSoundId(id);
	}
});
