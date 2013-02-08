$(function() {
	var audiofiles = [ 'iiyone1', 'iiyone2', 'kara', 'kirinsan1', 'kirinsan2', 'kirinsan3', 'kirinsan4', 'ne1', 'ne2', 'ne3', 'ne4', 'ne5', 'oshimai',
			'unvoa1', 'unvoa2', 'unvoa3', 'unvoa4', 'unvoa5', 'yeah1', 'yeah2', 'yeah3', 'yeah4', 'yonju1', 'yonju2', 'yonju3', 'yonju4', 'ze1', 'ze2', 'ze3',
			'ze4', 'ze5' ];

	$.each(audiofiles, function(idx, file) {
		$('<audio preload="auto">').attr('id', file).append($('<source type="audio/ogg"/>').attr('src', 'http://world.kirinsan.org/audio/' + file + '.ogg'))
				.append($('<source type="audio/mp4"/>').attr('src', 'http://world.kirinsan.org/audio/' + file + '.m4a')).appendTo(document.body);
	});

	$('#drumpad').on('touchstart', 'a', onClickPad);
});

function onClickPad() {
	var sounds = $(this).data('sound').split(',');
	var id = sounds[Math.floor(Math.random() * sounds.length)];
	android.play(id);
	
	if (android.isSocketEnabled()) {
		android.emit(id);
	}
}
