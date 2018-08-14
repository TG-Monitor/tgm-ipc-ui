package ai.quantumsense.tgmonitor.ipc.ui;

import ai.quantumsense.tgmonitor.cli.CliLifecycleHandler;
import ai.quantumsense.tgmonitor.corefacade.CoreFacade;
import ai.quantumsense.tgmonitor.ipc.api.serializer.pojo.Request;
import ai.quantumsense.tgmonitor.logincodeprompt.LoginCodePrompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ai.quantumsense.tgmonitor.ipc.api.Requests.*;

public class Endpoint implements CoreFacade, CliLifecycleHandler {

    private Logger logger = LoggerFactory.getLogger(Endpoint.class);

    private Connector connector;
    private RequestSender requestSender;

    public Endpoint(String amqpUri) {
        connector = new Connector(amqpUri);
        requestSender = new RequestSender(connector.getChannel());
    }

    @Override
    public void login(String phoneNumber, LoginCodePrompt loginCodePrompt) {
        requestSender.sendLogin(new Request(LOGIN, phoneNumber), loginCodePrompt);
    }

    @Override
    public void logout() {
        requestSender.send(new Request(LOGOUT));
    }

    @Override
    public boolean isLoggedIn() {
        return (boolean) requestSender.send(new Request(IS_LOGGED_IN)).getValue();
    }

    @Override
    public void start() {
        requestSender.send(new Request(START));
    }

    @Override
    public void stop() {
        requestSender.send(new Request(STOP));
    }

    @Override
    public boolean isRunning() {
        return (boolean) requestSender.send(new Request(IS_RUNNING)).getValue();
    }

    @Override
    public String getPhoneNumber() {
        return (String) requestSender.send(new Request(GET_PHONE_NUMBER)).getValue();
    }

    @Override
    public Set<String> getPeers() {
        return convertResponseToSet(requestSender.send(new Request(GET_PEERS)).getValue());
    }

    @Override
    public void setPeers(Set<String> peers) {
        requestSender.send(new Request(SET_PEERS, peers));
    }

    @Override
    public void addPeer(String peer) {
        requestSender.send(new Request(ADD_PEER, peer));
    }

    @Override
    public void addPeers(Set<String> peers) {
        requestSender.send(new Request(ADD_PEERS,  peers));
    }

    @Override
    public void removePeer(String peer) {
        requestSender.send(new Request(REMOVE_PEER, peer));
    }

    @Override
    public void removePeers(Set<String> peers) {
        requestSender.send(new Request(REMOVE_PEERS, peers));
    }

    @Override
    public Set<String> getPatterns() {
        return convertResponseToSet(requestSender.send(new Request(GET_PATTERNS)).getValue());
    }

    @Override
    public void setPatterns(Set<String> patterns) {
        requestSender.send(new Request(SET_PATTERNS, patterns));
    }

    @Override
    public void addPattern(String pattern) {
        requestSender.send(new Request(ADD_PATTERN, pattern));
    }

    @Override
    public void addPatterns(Set<String> patterns) {
        requestSender.send(new Request(ADD_PATTERNS, patterns));
    }

    @Override
    public void removePattern(String pattern) {
        requestSender.send(new Request(REMOVE_PATTERN, pattern));
    }

    @Override
    public void removePatterns(Set<String> patterns) {
        requestSender.send(new Request(REMOVE_PATTERNS, patterns));
    }

    @Override
    public Set<String> getEmails() {
        return convertResponseToSet(requestSender.send(new Request(GET_EMAILS)).getValue());
    }

    @Override
    public void setEmails(Set<String> emails) {
        requestSender.send(new Request(SET_EMAILS, emails));
    }

    @Override
    public void addEmail(String email) {
        requestSender.send(new Request(ADD_EMAIL, email));
    }

    @Override
    public void addEmails(Set<String> emails) {
        requestSender.send(new Request(ADD_EMAILS, emails));
    }

    @Override
    public void removeEmail(String email) {
        requestSender.send(new Request(REMOVE_EMAIL, email));
    }

    @Override
    public void removeEmails(Set<String> emails) {
        requestSender.send(new Request(REMOVE_EMAILS, emails));
    }

    @Override
    public void onCliCreate() {}

    @Override
    public void onCliDestroy() {
        connector.disconnect();
    }

    @SuppressWarnings("unchecked")
    private Set<String> convertResponseToSet(Object response) {
        return new HashSet<>((List<String>) response);
    }
}
