package com.example.myapplication.SignIn;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.example.myapplication.ResultCallBack;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.UserData;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SignInPresenterTest {

    @Mock
    private SignInView view;
    
    @Mock
    private SignInModel model;
    
    private SignInPresenter presenter;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        presenter = new SignInPresenter(view, model);
    }

    @Test
    public void testInitialize_CallsReload() {
        presenter.initialize();
        verify(model).ReloadUserAuth();
    }

    @Test
    public void testSignin_EmptyEmail_ShowsError() {
        presenter.signin("", "pass");
        verify(view).showShortMessage("input cannot be empty");
        verifyNoInteractions(model);
    }

    @Test
    public void testSignin_EmptyPassword_ShowsError() {
        presenter.signin("a@a.com", "");
        verify(view).showShortMessage("input cannot be empty");
        verifyNoInteractions(model);
    }

    @Test
    public void testSignin_Email_RoutesToParentProviderFlow() {
        SignInPresenter spyPresenter = spy(presenter);
        doReturn(true).when(spyPresenter).isEmail(anyString());
        doNothing().when(spyPresenter).signInForParentAndProvider(anyString(), anyString());

        spyPresenter.signin("test@test.com", "123");

        verify(spyPresenter).signInForParentAndProvider("test@test.com", "123");
    }

    @Test
    public void testSignin_Username_RoutesToChildFlow() {
        SignInPresenter spyPresenter = spy(presenter);
        doReturn(false).when(spyPresenter).isEmail(anyString());
        doNothing().when(spyPresenter).signInForChild(anyString(), anyString());

        spyPresenter.signin("childUser", "123");

        verify(spyPresenter).signInForChild("childUser", "123");
    }

    @Test
    public void testParentProvider_Failure_ShowsUserNotFound() {
        doAnswer(invocation -> {
            ResultCallBack<Boolean> cb = invocation.getArgument(2);
            cb.onComplete(false);
            return null;
        }).when(model).SignInAuth(anyString(), anyString(), any());

        presenter.signInForParentAndProvider("a@a.com", "123");

        verify(view).showShortMessage("User Not Found");
        verify(view, never()).GoToMainActivity();
        verify(view, never()).GoToOnBoardingActivity();
    }

    @Test
    public void testParentProvider_Success_GoesToCorrectActivity() {
        // Simulate successful sign-in
        doAnswer(invocation -> {
            ResultCallBack<Boolean> cb = invocation.getArgument(2);
            cb.onComplete(true);
            return null;
        }).when(model).SignInAuth(anyString(), anyString(), any());

        when(model.GetCurrentUIDAuth()).thenReturn("123");

        // Fake returned user with firstTime=true
        UserData fake = mock(UserData.class);
        when(fake.getFirstTime()).thenReturn(true);

        // Simulate DB lookup
        doAnswer(invocation -> {
            ResultCallBack<UserData> cb = invocation.getArgument(1);
            cb.onComplete(fake);
            return null;
        }).when(model).QueryDBforNonChildren(eq("123"), any());

        presenter.signInForParentAndProvider("a@a.com", "123");

        verify(view).showShortMessage("Welcome!");
        verify(view).GoToOnBoardingActivity();
        verify(view, never()).GoToMainActivity();
    }

    @Test
    public void testParentProvider_Success_FirstTimeFalse_GoesToMainActivity() {
        // Simulate successful sign-in
        doAnswer(invocation -> {
            ResultCallBack<Boolean> cb = invocation.getArgument(2);
            cb.onComplete(true);
            return null;
        }).when(model).SignInAuth(anyString(), anyString(), any());

        when(model.GetCurrentUIDAuth()).thenReturn("123");

        // Fake returned user with firstTime=false
        UserData fake = mock(UserData.class);
        when(fake.getFirstTime()).thenReturn(false);

        // Simulate DB lookup
        doAnswer(invocation -> {
            ResultCallBack<UserData> cb = invocation.getArgument(1);
            cb.onComplete(fake);
            return null;
        }).when(model).QueryDBforNonChildren(eq("123"), any());

        presenter.signInForParentAndProvider("a@a.com", "123");

        verify(view).showShortMessage("Welcome!");
        verify(view).GoToMainActivity();
        verify(view, never()).GoToOnBoardingActivity();
    }

    @Test
    public void testChild_UserNotFound() {
        doAnswer(invocation -> {
            ResultCallBack<String> cb = invocation.getArgument(1);
            cb.onComplete(""); // no parent ID → not found
            return null;
        }).when(model).usernameExists(anyString(), any());

        presenter.signInForChild("child", "111");

        verify(view).showShortMessage("User Not Found");
        verify(view, never()).GoToMainActivity();
        verify(view, never()).GoToOnBoardingActivity();
    }

    @Test
    public void testChild_WrongPassword_ShowsError() {
        doAnswer(invocation -> {
            ResultCallBack<String> cb = invocation.getArgument(1);
            cb.onComplete("parentID");
            return null;
        }).when(model).usernameExists(anyString(), any());

        ChildAccount fakeChild = mock(ChildAccount.class);
        when(fakeChild.getPassword()).thenReturn("correctPass");

        doAnswer(invocation -> {
            ResultCallBack<UserData> cb = invocation.getArgument(2);
            cb.onComplete(fakeChild);
            return null;
        }).when(model).QueryDBforChildren(eq("parentID"), anyString(), any());

        presenter.signInForChild("child", "wrongPass");

        verify(view).showShortMessage("User Not Found");
        verify(view, never()).GoToMainActivity();
        verify(view, never()).GoToOnBoardingActivity();
    }

    @Test
    public void testChild_Success_GoesToCorrectActivity() {
        doAnswer(invocation -> {
            ResultCallBack<String> cb = invocation.getArgument(1);
            cb.onComplete("parentID");
            return null;
        }).when(model).usernameExists(anyString(), any());

        ChildAccount fakeChild = mock(ChildAccount.class);
        when(fakeChild.getPassword()).thenReturn("correctPass");
        when(fakeChild.getFirstTime()).thenReturn(true);

        doAnswer(invocation -> {
            ResultCallBack<UserData> cb = invocation.getArgument(2);
            cb.onComplete(fakeChild);
            return null;
        }).when(model).QueryDBforChildren(eq("parentID"), anyString(), any());

        presenter.signInForChild("child", "correctPass");

        verify(view).showShortMessage("Welcome!");
        verify(view).GoToOnBoardingActivity();
        verify(view, never()).GoToMainActivity();
    }
}
