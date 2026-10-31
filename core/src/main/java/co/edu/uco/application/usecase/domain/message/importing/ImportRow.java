package co.edu.uco.application.usecase.domain.message.importing;

public record ImportRow(int rowNumber, String code, String functionality, String type,
                        String category, String status, String title, String content) {
}
