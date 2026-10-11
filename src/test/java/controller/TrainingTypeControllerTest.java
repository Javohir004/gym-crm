package controller;

import com.epam.training.gym.controller.TrainingTypeController;
import com.epam.training.gym.facade.GymFacade;
import com.epam.training.gym.model.TrainingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrainingTypeControllerTest {

    @Mock
    private GymFacade facade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TrainingTypeController(facade)).build();
    }

    @Test
    void getAll_returns200WithList_andNeedsNoAuthorization() throws Exception {
        when(facade.getTrainingTypes()).thenReturn(List.of(
                TrainingType.builder().id(1L).trainingTypeName("Fitness").build(),
                TrainingType.builder().id(2L).trainingTypeName("Yoga").build()));

        mockMvc.perform(get("/api/training-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingType").value("Fitness"))
                .andExpect(jsonPath("$[0].trainingTypeId").value(1))
                .andExpect(jsonPath("$[1].trainingType").value("Yoga"));
    }
}