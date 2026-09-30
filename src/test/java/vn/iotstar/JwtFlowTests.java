package vn.iotstar;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class JwtFlowTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void signupLoginAndAccessProtectedEndpoint() throws Exception {
		mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"trung@hcmute.edu.vn\",\"password\":\"123456\",\"fullName\":\"Nguyen Huu Trung\",\"images\":\"/images/u1.jpg\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email", is("trung@hcmute.edu.vn")));

		String body = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"trung@hcmute.edu.vn\",\"password\":\"123456\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.expiresIn", is(3600000)))
			.andReturn().getResponse().getContentAsString();
		JsonNode json = objectMapper.readTree(body);
		String token = json.get("token").asText();

		mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName", is("Nguyen Huu Trung")));

		mockMvc.perform(get("/users/").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk());

		// Sai mat khau
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"trung@hcmute.edu.vn\",\"password\":\"sai\"}"))
			.andExpect(status().isUnauthorized());

		// Khong gui token
		mockMvc.perform(get("/users/me"))
			.andExpect(status().isForbidden());

		// Token bi sua chu ky
		String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");
		mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + tampered))
			.andExpect(status().isUnauthorized());

		// Chuoi token sai dinh dang
		mockMvc.perform(get("/users/me").header("Authorization", "Bearer dfdfdfdfd"))
			.andExpect(status().isUnauthorized());
	}
}
