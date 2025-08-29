package com.codersguidebook.supernova.exception

class PlaylistNotFoundException(playlistName: String) :
    RuntimeException("The following playlist could not be found: $playlistName")