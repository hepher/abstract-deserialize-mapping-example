package com.enelxway.cucumber.features.steps;

import com.enelxway.cucumber.document.User;
import com.enelxway.cucumber.CucumberSpringConfiguration;
import com.enelxway.cucumber.service.UserService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class UserSteps {

    @Autowired
    private UserService userService;

    private User user;

    @Given("A not exist user {string}")
    public void aNotExistUser(String userId) {
        user = userService.findById(userId);
    }

    @When("the user is searched")
    public void theUserIsSearched() {

    }

    @Then("An error with code {int} return")
    public void anErrorWithCodeReturn(Integer errorCode) {

    }
}
