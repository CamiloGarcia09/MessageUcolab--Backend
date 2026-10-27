package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.FunctionalityRepository;
import co.edu.uco.application.usecase.handling.HandlingCreateFunctionalityPort;
import co.edu.uco.application.usecase.validator.functionality.CreateFunctionalityCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.helpers.UtilUUID;


public final class CreateFunctionalityUseCase implements HandlingCreateFunctionalityPort {

    private final FunctionalityRepository functionalityRepository;
    private final CreateFunctionalityCompositeValidator validator;
    private final LoggingPort log;

    public CreateFunctionalityUseCase(FunctionalityRepository functionalityRepository,
                                      CreateFunctionalityCompositeValidator validator,
                                      LoggingPortFactory loggerFactory) {
        this.functionalityRepository = functionalityRepository;
        this.validator = validator;
        this.log = loggerFactory.getLogger(CreateFunctionalityUseCase.class);
    }

    @Override
    public void createFunctionality(CreateFunctionalityDTO dto) {
        validator.validate(dto);

        try {
            var application = ApplicationData.build(UtilUUID.getStringToUUID(dto.getApplicationId()), "");
            var functionality = new FunctionalityData(
                    UtilUUID.getNewUUID(),
                    dto.getName(),
                    application
            );
            functionalityRepository.create(functionality, dto.getStateId());
            log.info("Functionality created successfully with name: {}", dto.getName());
        } catch (CrossWordsException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error creating functionality in repository", ex);
            throw BusinessException.buildTechnicalException(
                    "Error al crear la funcionalidad", ex, ExceptionLocation.APPLICATION);
        }
    }

}
