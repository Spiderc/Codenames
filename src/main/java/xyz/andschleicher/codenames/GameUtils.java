package xyz.andschleicher.codenames;

import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import javax.servlet.http.HttpServletRequest;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import xyz.andschleicher.codenames.bean.Player;
import xyz.andschleicher.codenames.controller.PlayerController;

public class GameUtils {
	private static final String JWT_KEY = "eka8BGYzilv03BMT";
	public static String startingTeam;
	public static String currentTurn;

	public static String generateJWT(String username) {
		String jwt = null;
		jwt = Jwts.builder()
				.setSubject(username)
				.setExpiration(new Date(System.currentTimeMillis() + (30 * 60 * 1000)))
				.signWith(SignatureAlgorithm.HS512, JWT_KEY.getBytes())
				.compact();
		return jwt;
	}
	
	public static Player playerFromJWT(HttpServletRequest request) {
		Player player = null;
		
		String authToken = request.getHeader("Authorization");
		if(authToken != null) {
			try {
				String username = Jwts.parser()
						.setSigningKey(JWT_KEY.getBytes())
						.parseClaimsJws(authToken.replace("Bearer ", ""))
						.getBody()
						.getSubject();
				
				if(username != null) {
					PlayerController playerController = new PlayerController();
					player = playerController.getPlayer(username);
				}
			} catch (ExpiredJwtException e) {
				
			}
		}
		
		return player;
	}
	
	public static int randomNumber(int min, int max) {
		return ThreadLocalRandom.current().nextInt(min, max+1);
	}
}
