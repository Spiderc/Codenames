package xyz.andschleicher.codenames.controller;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import xyz.andschleicher.codenames.GameUtils;
import xyz.andschleicher.codenames.HibernateUtil;
import xyz.andschleicher.codenames.bean.User;

@RestController
@RequestMapping(value="/user")
public class UserController {

	@SuppressWarnings("unchecked")
	@RequestMapping(value="/get", method=RequestMethod.GET)
	public List<User> getUsers(){
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		
		Query query = session.createQuery("from User");
		
		List<User> list = query.list();
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public User getUser(String username){
		User result = null;
		
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		
		Query query = session.createQuery("from User where username=:username");
		query.setParameter("username", username);
		
		List<User> list = query.list();
		if(!list.isEmpty()) {
			result = list.get(0);
		}
		return result;
	}
	
	@RequestMapping(value="/add", method=RequestMethod.POST)
	public void addUser(@RequestBody User user){
		try {
			Session session = HibernateUtil.getSessionFactory().openSession();
			session.beginTransaction();
			
			SecureRandom random = new SecureRandom();
			byte[] saltBytes = new byte[64];
			random.nextBytes(saltBytes);
			
			user.setSalt(new String(Base64.decodeBase64(saltBytes), "UTF-8"));
			user.setPassword(DigestUtils.sha1Hex(user.getPassword() + user.getSalt()));
			
			session.save(user);
			session.getTransaction().commit();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value="/auth", method=RequestMethod.POST)
	public void authUser(@RequestBody User user, HttpServletResponse response){
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		
		Query userQuery = session.createQuery("from User where username=:username");
		userQuery.setParameter("username", user.getUsername());
		
		session.getTransaction().commit();
		List<User> userList = userQuery.list();
		String userSalt = "";
		if(!userList.isEmpty()) {
			userSalt = userList.get(0).getSalt();
		}
		session.close();
		
		session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		
		Query query = session.createQuery("from User where username=:username and password=:password");
		query.setParameter("username", user.getUsername());
		query.setParameter("password", DigestUtils.sha1Hex(user.getPassword() + userSalt));
		
		session.getTransaction().commit();
		List<User> list = query.list();
		if(!list.isEmpty()) {
			response.setHeader("Authorization", "Bearer " + GameUtils.generateJWT(user.getUsername()));
		} else {
			response.setStatus(401);
		}
		session.close();
	}
}
