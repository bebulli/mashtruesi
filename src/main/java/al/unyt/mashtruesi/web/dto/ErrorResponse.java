package al.unyt.mashtruesi.web.dto;

public record ErrorResponse(int status, String error, String message) {
}
